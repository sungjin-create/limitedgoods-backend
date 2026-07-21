package com.limitedgoods.limitedgoods.order.application.payment;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidEvent;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidItem;
import com.limitedgoods.limitedgoods.order.application.mapper.OrderResponseMapper;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.payment.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OutboxEventService outboxEventService;
    private final OrderItemRepository orderItemRepository;
    private final OrderResponseMapper orderResponseMapper;

    @Transactional(readOnly = true)
    public OrderPaymentInfo getPaymentInfo(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
    }

    @Transactional
    public OrderPaymentInfo startPayment(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);
        order.markPaymentPending(LocalDateTime.now());
        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
    }

    @Transactional
    public void markPaymentApproved(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.PAYMENT_APPROVED
                || order.getStatus() == OrderStatus.PAID) {
            return;
        }

        order.markPaymentApproved();
    }

    @Transactional
    public OrderResponseDto finalizeApprovedPayment(Long userId, Long orderId) {

        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.PAID) {
            return orderResponseMapper.toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PAYMENT_APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "현재 주문 상태 = " + order.getStatus());
        }

        order.markPaid();

        //product의 soldOut 재고 갯수 업데이트
        updateProductSoldCount(userId, orderId);

        outboxEventService.save(
                OutboxEventType.ORDER_PAID,
                "ORDER",
                order.getId(),
                new OrderPaidEvent(
                        order.getId(),
                        userId,
                        order.getUser().getEmail(),
                        order.getTotalPrice(),
                        LocalDateTime.now(),
                        orderItemRepository.findByOrderId(order.getId()).stream()
                                .map(item -> new OrderPaidItem(
                                        item.getProduct().getId(),
                                        item.getQuantity(),
                                        item.getPrice()
                                ))
                                .toList()
                )
        );

        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public void failPayment(Long userId, Long orderId, String reason) {
        Order order = getOrder(orderId, userId);
        order.markPaymentFailed(reason);
    }

    private Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    private Order getOrderForUpdate(Long orderId, Long userId) {
        Order order = orderRepository.findByIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    private void updateProductSoldCount(Long userId, Long orderId) {
        List<OrderItem> orderItemList = orderRepository.findOrderItemsByOrder(orderId, userId);

        for(OrderItem orderItem : orderItemList) {
            productRepository.increaseSoldCount(
                    orderItem.getProduct().getId(),
                    orderItem.getQuantity());
        }
    }
}

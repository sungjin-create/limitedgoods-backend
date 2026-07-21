package com.limitedgoods.limitedgoods.order.application.cancel;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.event.payload.order.OrderCanceledEvent;
import com.limitedgoods.limitedgoods.order.application.mapper.OrderResponseMapper;
import com.limitedgoods.limitedgoods.order.application.payment.OrderPaymentService;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.payment.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventService outboxEventService;
    private final OrderResponseMapper orderResponseMapper;
    private final OrderPaymentService orderPaymentService;
    private final ProductSoldOutCacheService productSoldOutCacheService;

    @Transactional
    public OrderResponseDto cancelPaidOrder(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.cancelPaidOrder();

        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public OrderPaymentInfo requestCancel(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        if (order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
            return orderPaymentService.getPaymentInfo(userId, orderId);
        }

        if (order.getStatus() == OrderStatus.CANCEL_FAILED) {
            return orderPaymentService.getPaymentInfo(userId, orderId);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.requestCancel();

        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
    }

    @Transactional
    public OrderResponseDto completeRefund(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            return orderResponseMapper.toResponse(order);
        }

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED
                && order.getStatus() != OrderStatus.CANCEL_FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "현재 주문 상태 = " + order.getStatus());
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProduct().getId();

            productRepository.increaseStock(
                    productId,
                    orderItem.getQuantity()
            );
            productSoldOutCacheService.clearSoldOutAfterCommit(productId);
        }

        order.markRefunded();

        outboxEventService.save(
                OutboxEventType.ORDER_CANCELED,
                "ORDER",
                order.getId(),
                new OrderCanceledEvent(order.getId(), userId, LocalDateTime.now())
        );

        return orderResponseMapper.toResponse(order);
    }

    @Transactional
    public void failRefund(Long userId, Long orderId, String reason) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            return;
        }

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "현재 주문 상태 = " + order.getStatus());
        }

        order.markCancelFailed(reason);
    }

    private Order getOrderForUpdate(Long orderId, Long userId) {
        Order order = orderRepository.findByIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }
}

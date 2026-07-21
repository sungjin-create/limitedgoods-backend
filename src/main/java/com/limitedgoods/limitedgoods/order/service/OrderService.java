package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrderDetail(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        return toDetailResponse(order, items.get(0));
    }

    private Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }



    private OrderDetailResponseDto toDetailResponse(Order order, OrderItem item) {
        return new OrderDetailResponseDto(order.getId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                item != null ? item.getProduct().getId() : null,
                item != null ? item.getProduct().getName() : null,
                item != null ? item.getQuantity() : 0,
                item != null ? item.getPrice() : 0
                );
    }

    private OrderResponseDto toResponse(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
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

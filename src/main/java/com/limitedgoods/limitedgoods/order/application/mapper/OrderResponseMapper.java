package com.limitedgoods.limitedgoods.order.application.mapper;

import com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderItemResponse;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderResponseMapper {
    public OrderResponseDto toResponse(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    public OrderDetailResponseDto toDetailResponse(
            Order order,
            List<OrderItem> orderItems
    ) {
        List<OrderItemResponse> itemResponses =
                orderItems.stream()
                        .map(item ->
                                new OrderItemResponse(
                                        item.getProduct().getId(),
                                        item.getProduct().getName(),
                                        item.getQuantity(),
                                        item.getPrice(),
                                        item.getLineTotalPrice()
                                )
                        )
                        .toList();

        return new OrderDetailResponseDto(
                order.getId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                itemResponses
        );
    }
}

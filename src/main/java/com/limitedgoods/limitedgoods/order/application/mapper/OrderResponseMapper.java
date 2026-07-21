package com.limitedgoods.limitedgoods.order.application.mapper;

import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import org.springframework.stereotype.Component;

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
}

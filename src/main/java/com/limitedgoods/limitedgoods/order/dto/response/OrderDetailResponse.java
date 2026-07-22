package com.limitedgoods.limitedgoods.order.dto.response;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(

        Long orderId,
        long totalPrice,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        List<OrderItemResponse> items
){

}


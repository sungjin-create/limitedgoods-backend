package com.limitedgoods.limitedgoods.order.dto.response;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderDetailResponseDto (

     Long orderId,
     long totalPrice,
     OrderStatus status,
     LocalDateTime createdAt,
     LocalDateTime expiresAt,
     Long productId,
     String productName,
     int quantity,
     int unitPrice
){

}


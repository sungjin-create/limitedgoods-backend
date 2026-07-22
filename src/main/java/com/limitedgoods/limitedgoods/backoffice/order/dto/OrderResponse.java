package com.limitedgoods.limitedgoods.backoffice.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;


public record OrderResponse (
    long orderId,
    String userEmail,
    long totalPrice,
    OrderStatus status,
    List<OrderItemResponse> orderItems,
    LocalDateTime createdAt
)
{

}

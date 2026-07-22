package com.limitedgoods.limitedgoods.backoffice.order.dto;

import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderStatusResponse(
        Long orderId,
        OrderStatus status
) {
    public static OrderStatusResponse from(Order order){
        return new OrderStatusResponse(order.getId(), order.getStatus());
    }
}

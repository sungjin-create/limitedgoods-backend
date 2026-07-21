package com.limitedgoods.limitedgoods.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderPaymentInfo(
        Long orderId,
        long totalPrice,
        OrderStatus orderStatus
) {
}

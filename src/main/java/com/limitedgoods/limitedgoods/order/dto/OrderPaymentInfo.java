package com.limitedgoods.limitedgoods.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderPaymentInfo(
        Long orderId,
        int totalPrice,
        OrderStatus orderStatus
) {
}

package com.limitedgoods.limitedgoods.order.application.payment.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderPaymentInfo(
        Long orderId,
        long totalPrice,
        OrderStatus orderStatus
) {
}

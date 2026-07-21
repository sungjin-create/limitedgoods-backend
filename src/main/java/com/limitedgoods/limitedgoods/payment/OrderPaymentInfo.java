package com.limitedgoods.limitedgoods.payment;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderPaymentInfo(
        Long orderId,
        long totalPrice,
        OrderStatus orderStatus
) {
}

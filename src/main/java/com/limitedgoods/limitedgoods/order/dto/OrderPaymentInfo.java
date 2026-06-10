package com.limitedgoods.limitedgoods.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

public record OrderPaymentInfo(
        Long orderId,
        Long productId,
        int quantity,
        int totalPrice,
        OrderStatus orderStatus
) {
}

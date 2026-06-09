package com.limitedgoods.limitedgoods.order.dto;

public record OrderPaymentInfo(
        Long orderId,
        Long productId,
        int quantity,
        int totalPrice
) {
}

package com.limitedgoods.limitedgoods.order.application.payment.dto;

import com.limitedgoods.limitedgoods.order.dto.response.OrderResponse;

public record PaymentStartResult(
        PaymentStartAction action,
        Long orderId,
        long amount,
        Long paymentAttemptId,
        String idempotencyKey,
        OrderResponse completedOrder
) {
}
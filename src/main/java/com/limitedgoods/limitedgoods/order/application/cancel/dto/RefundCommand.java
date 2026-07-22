package com.limitedgoods.limitedgoods.order.application.cancel.dto;

public record RefundCommand(
        Long userId,
        Long orderId,
        String pgTransactionId,
        long amount,
        String idempotencyKey
) {
}

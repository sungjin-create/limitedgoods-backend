package com.limitedgoods.limitedgoods.event.payload;

import java.time.LocalDateTime;

public record OrderCanceledEvent(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        LocalDateTime canceledAt
) {
}
package com.limitedgoods.limitedgoods.event.payload.order;

import java.time.LocalDateTime;

public record OrderCanceledEvent(
        Long orderId,
        Long userId,
        LocalDateTime canceledAt
) {
}
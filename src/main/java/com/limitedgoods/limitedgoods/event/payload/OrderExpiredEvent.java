package com.limitedgoods.limitedgoods.event.payload;

import java.time.LocalDateTime;

public record OrderExpiredEvent(
        Long orderId,
        Long productId,
        int quantity,
        LocalDateTime expiredAt
) {
}

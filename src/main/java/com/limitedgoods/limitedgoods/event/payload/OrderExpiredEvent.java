package com.limitedgoods.limitedgoods.event.payload;

import java.time.LocalDateTime;

public record OrderExpiredEvent(
        Long orderId,
        LocalDateTime expiredAt
) {
}

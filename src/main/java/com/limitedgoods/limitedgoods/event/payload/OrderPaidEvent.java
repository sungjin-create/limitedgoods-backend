package com.limitedgoods.limitedgoods.event.payload;

import java.time.LocalDateTime;

public record OrderPaidEvent(
        Long orderId,
        Long userId,
        int totalPrice,
        LocalDateTime paidAt
) {
}

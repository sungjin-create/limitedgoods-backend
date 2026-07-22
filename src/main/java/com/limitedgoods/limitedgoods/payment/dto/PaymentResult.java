package com.limitedgoods.limitedgoods.payment.dto;

import java.time.LocalDateTime;

public record PaymentResult(
        String transactionId,
        long approvedAmount,
        LocalDateTime approvedAt
) {
}

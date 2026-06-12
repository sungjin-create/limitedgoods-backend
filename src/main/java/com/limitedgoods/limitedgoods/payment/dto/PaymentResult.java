package com.limitedgoods.limitedgoods.payment.dto;

import java.time.LocalDateTime;

public record PaymentResult(
        String transactionId,
        LocalDateTime approvedAt
) {}

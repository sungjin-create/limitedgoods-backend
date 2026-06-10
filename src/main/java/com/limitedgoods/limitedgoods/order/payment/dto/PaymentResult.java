package com.limitedgoods.limitedgoods.order.payment.dto;

import java.time.LocalDateTime;

public record PaymentResult(
        String transactionId,
        LocalDateTime approvedAt
) {}

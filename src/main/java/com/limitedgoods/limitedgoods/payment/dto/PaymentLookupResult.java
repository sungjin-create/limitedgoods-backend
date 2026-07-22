package com.limitedgoods.limitedgoods.payment.dto;

import java.time.LocalDateTime;

public record PaymentLookupResult(
        PaymentLookupStatus status,
        String transactionId,
        long approvedAmount,
        LocalDateTime approvedAt,
        String failureCode,
        String failureReason
) {
    public PaymentResult toPaymentResult() {
        return new PaymentResult(
                transactionId,
                approvedAmount,
                approvedAt
        );
    }
}

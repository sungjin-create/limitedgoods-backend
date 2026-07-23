package com.limitedgoods.limitedgoods.payment.metrics;

public record PaymentMetricEvent(
        String result,
        String reason,
        long amount
) {

    public static PaymentMetricEvent success(long amount) {
        return new PaymentMetricEvent(
                "success",
                "none",
                amount
        );
    }

    public static PaymentMetricEvent declined() {
        return new PaymentMetricEvent(
                "failure",
                "pg_declined",
                0L
        );
    }
}
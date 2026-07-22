package com.limitedgoods.limitedgoods.payment.exception;

public class PaymentDeclinedException extends RuntimeException {

    private final String failureCode;

    public PaymentDeclinedException(
            String failureCode,
            String message
    ) {
        super(message);
        this.failureCode = failureCode;
    }

    public String getFailureCode() {
        return failureCode;
    }
}

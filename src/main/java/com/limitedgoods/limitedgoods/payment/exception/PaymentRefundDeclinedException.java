package com.limitedgoods.limitedgoods.payment.exception;

public class PaymentRefundDeclinedException extends RuntimeException {

    public PaymentRefundDeclinedException(String message) {
        super(message);
    }
}

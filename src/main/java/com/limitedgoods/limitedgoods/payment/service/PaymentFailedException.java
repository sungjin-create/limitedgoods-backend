package com.limitedgoods.limitedgoods.payment.service;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}

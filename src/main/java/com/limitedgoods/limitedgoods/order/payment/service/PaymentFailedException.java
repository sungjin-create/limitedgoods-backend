package com.limitedgoods.limitedgoods.order.payment.service;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}

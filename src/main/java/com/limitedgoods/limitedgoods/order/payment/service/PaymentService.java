package com.limitedgoods.limitedgoods.order.payment.service;

import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;

public interface PaymentService {
    void pay(Long orderId, int amount, PaymentRequestDto request);
}

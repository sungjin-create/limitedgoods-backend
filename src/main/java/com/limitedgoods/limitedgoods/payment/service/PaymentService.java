package com.limitedgoods.limitedgoods.payment.service;

import com.limitedgoods.limitedgoods.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.payment.dto.PaymentResult;

public interface PaymentService {
    PaymentResult pay(Long orderId, Long amount, PaymentRequestDto request);

    void cancel(Long orderId, Long amount);
}

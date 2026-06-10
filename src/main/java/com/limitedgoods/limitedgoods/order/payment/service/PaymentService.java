package com.limitedgoods.limitedgoods.order.payment.service;

import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentResult;

public interface PaymentService {
    PaymentResult pay(Long orderId, int amount, PaymentRequestDto request);
}

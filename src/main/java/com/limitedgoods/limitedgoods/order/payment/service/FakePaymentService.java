package com.limitedgoods.limitedgoods.order.payment.service;

import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FakePaymentService implements PaymentService {

    @Override
    public PaymentResult pay(Long orderId, int amount, PaymentRequestDto request) {
        if(request.isForceFail()){
            throw new PaymentFailedException("결제 승인 실패");
        }
        return new PaymentResult(
                UUID.randomUUID().toString(),
                LocalDateTime.now()
        );
    }

}

package com.limitedgoods.limitedgoods.payment.service;

import com.limitedgoods.limitedgoods.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.payment.dto.PaymentResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FakePaymentService implements PaymentService {

    @Override
    public PaymentResult pay(Long orderId, Long amount, PaymentRequestDto request) {
        if(request.isForceFail()){
            throw new PaymentFailedException("결제 승인 실패");
        }
        return new PaymentResult(
                UUID.randomUUID().toString(),
                LocalDateTime.now()
        );
    }

    @Override
    public void cancel(Long orderId, Long amount) {
        if (orderId % 5 == 0) {
            throw new PaymentFailedException("PG 환불 실패");
        }
    }

}

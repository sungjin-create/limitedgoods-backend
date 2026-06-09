package com.limitedgoods.limitedgoods.order.payment.service;

import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import org.springframework.stereotype.Service;

@Service
public class FakePaymentService implements PaymentService {

    @Override
    public void pay(Long orderId, int amount, PaymentRequestDto request) {
        if(request.isForceFail()){
            throw new PaymentFailedException("결제 승인 실패");
        }
    }

}

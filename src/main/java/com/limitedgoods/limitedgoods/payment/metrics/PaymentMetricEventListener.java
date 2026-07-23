package com.limitedgoods.limitedgoods.payment.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentMetricEventListener {

    private final PaymentMetrics paymentMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePayment(PaymentMetricEvent event) {
        paymentMetrics.recordPayment(event.result(), event.reason());
        if ("success".equals(event.result())) {
            paymentMetrics.recordRevenue(event.amount());
        }
    }
}
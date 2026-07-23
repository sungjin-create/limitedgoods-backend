package com.limitedgoods.limitedgoods.order.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderMetricEventListener {

    private final OrderMetrics orderMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreateMetricEvent event) {
        orderMetrics.recordOrderCreate(event.result(), event.reason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderExpired(OrderExpiredMetricEvent event) {
        orderMetrics.recordOrderExpired(event.reason());
    }
}
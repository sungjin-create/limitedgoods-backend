package com.limitedgoods.limitedgoods.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter orderCreatedCounter;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = Counter.builder("limitedgoods_order_created")
                .description("Created order count")
                .register(meterRegistry);
    }

    public void increaseOrderCreated() {
        orderCreatedCounter.increment();
    }
}
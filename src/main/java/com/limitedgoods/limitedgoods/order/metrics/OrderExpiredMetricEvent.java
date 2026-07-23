package com.limitedgoods.limitedgoods.order.metrics;

public record OrderExpiredMetricEvent(
        String reason
) {

    public static OrderExpiredMetricEvent timeout() {
        return new OrderExpiredMetricEvent(
                "payment_timeout"
        );
    }

    public static OrderExpiredMetricEvent replacedByNewOrder() {
        return new OrderExpiredMetricEvent(
                "replaced_by_new_order"
        );
    }
}

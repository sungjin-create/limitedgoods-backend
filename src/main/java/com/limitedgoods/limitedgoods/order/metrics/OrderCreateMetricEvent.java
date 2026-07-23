package com.limitedgoods.limitedgoods.order.metrics;

public record OrderCreateMetricEvent(
        String result,
        String reason
) {

    public static OrderCreateMetricEvent success() {
        return new OrderCreateMetricEvent(
                "success",
                "none"
        );
    }

}
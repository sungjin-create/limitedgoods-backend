package com.limitedgoods.limitedgoods.backoffice.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BackofficeBusinessMetricResponse {

    private final long todayOrderCount;
    private final long todayPaidOrderCount;
    private final long todayRevenue;
    private final long soldOutProductCount;
}
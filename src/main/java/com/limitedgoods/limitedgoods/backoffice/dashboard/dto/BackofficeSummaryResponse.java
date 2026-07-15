package com.limitedgoods.limitedgoods.backoffice.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackofficeSummaryResponse {

    private long todayOrderCount;
    private double orderGrowthRate;

    private long todayRevenue;
    private double revenueGrowthRate;

    private long todayPaidOrderCount;
    private double paymentCompletionRate;

    private long lowStockProductCount;
    private long soldOutProductCount;

}

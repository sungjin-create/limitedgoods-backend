package com.limitedgoods.limitedgoods.backoffice.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BackofficeResponse {
    private BackofficeSummaryResponse summary;
    private BackofficeOrderFlowResponse orderFlow;
    private List<BackofficeRecentOrderResponse> recentOrders;
    private List<BackofficeAlertResponse> alerts;
}

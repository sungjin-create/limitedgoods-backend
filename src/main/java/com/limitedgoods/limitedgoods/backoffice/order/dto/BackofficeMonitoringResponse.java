package com.limitedgoods.limitedgoods.backoffice.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BackofficeMonitoringResponse {

    private BackofficeMonitoringSummaryResponse summary;
    private List<BackofficeMonitoringOrdersResponse> orders;

}

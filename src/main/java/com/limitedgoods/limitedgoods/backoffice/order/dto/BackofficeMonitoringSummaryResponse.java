package com.limitedgoods.limitedgoods.backoffice.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackofficeMonitoringSummaryResponse {
    private long paymentPendingCount;
    private long paidCount;
    private long cancelRequestedCount;
    private long paymentFailedCount;
}

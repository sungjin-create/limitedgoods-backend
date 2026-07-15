package com.limitedgoods.limitedgoods.backoffice.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackofficeOrderFlowResponse {
    private long createdCount;
    private long paidCount;
    private long pendingCount;
    private long failedOrExpiredCount;
}

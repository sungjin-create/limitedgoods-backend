package com.limitedgoods.limitedgoods.backoffice.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackofficeAlertResponse {
    private String level;  // critical, warning, info
    private String title;
    private String detail;
    private String time;
}

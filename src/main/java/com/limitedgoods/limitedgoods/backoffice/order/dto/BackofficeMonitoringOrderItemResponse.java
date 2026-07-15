package com.limitedgoods.limitedgoods.backoffice.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackofficeMonitoringOrderItemResponse {
    private long productId;
    private String productName;
    private long quantity;
    private long price;
}

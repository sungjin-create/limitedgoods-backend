package com.limitedgoods.limitedgoods.backoffice.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class BackofficeMonitoringOrdersResponse {
    private long orderId;
    private String userEmail;
    private long totalPrice;
    private String status;
    private List<BackofficeMonitoringOrderItemResponse> orderItems;
    private LocalDateTime createdAt;
}

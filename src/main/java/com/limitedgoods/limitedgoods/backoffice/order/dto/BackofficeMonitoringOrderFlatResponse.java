package com.limitedgoods.limitedgoods.backoffice.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class BackofficeMonitoringOrderFlatResponse {
    private Long orderId;
    private String email;
    private long totalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;

    private Long productId;
    private String productName;
    private int quantity;
    private int price;
}

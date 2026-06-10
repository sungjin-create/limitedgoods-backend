package com.limitedgoods.limitedgoods.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderDetailResponseDto {

    private Long orderId;
    private int totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private Long productId;
    private String productName;
    private int quantity;
    private int unitPrice;
}

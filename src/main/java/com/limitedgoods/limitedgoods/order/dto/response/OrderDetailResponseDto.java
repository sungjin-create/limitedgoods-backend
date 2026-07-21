package com.limitedgoods.limitedgoods.order.dto.response;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter

public class OrderDetailResponseDto {

    private Long orderId;
    private long totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private Long productId;
    private String productName;
    private int quantity;
    private int unitPrice;

    public OrderDetailResponseDto(
            Long orderId,
            long totalPrice,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            Long productId,
            String productName,
            int quantity,
            int unitPrice
    ) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status.name();
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}

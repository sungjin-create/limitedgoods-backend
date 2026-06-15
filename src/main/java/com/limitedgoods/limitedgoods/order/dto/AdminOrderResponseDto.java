package com.limitedgoods.limitedgoods.order.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminOrderResponseDto {

    private Long orderId;
    private Long userId;
    private String userEmail;

    private Long productId;
    private String productName;

    private int quantity;
    private int unitPrice;
    private int totalPrice;

    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;

    public AdminOrderResponseDto(
            Long orderId,
            Long userId,
            String userEmail,
            Long productId,
            String productName,
            int quantity,
            int unitPrice,
            int totalPrice,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime expiresAt
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.expiresAt = expiresAt;
    }
}

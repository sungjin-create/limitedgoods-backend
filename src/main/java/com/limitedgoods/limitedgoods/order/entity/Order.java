package com.limitedgoods.limitedgoods.order.entity;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private int totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private LocalDateTime expiresAt;
    private String failReason;

    public static Order create(User user, int totalPrice, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();

        return Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    public void markPaymentPending() {
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.PAYMENT_PENDING;
        this.updatedAt = LocalDateTime.now();

        this.failReason = null;
        this.failedAt = null;
    }

    public void markExpired() {
        if (this.status != OrderStatus.CREATED
                && this.status != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        this.status = OrderStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaid() {
        validateCurrentStatus(OrderStatus.PAYMENT_APPROVED);
        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.PAID;
        this.paidAt = now;
        this.updatedAt = now;
    }

    public void markPaymentFailed(String reason) {
        validateCurrentStatus(OrderStatus.PAYMENT_PENDING);
        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.PAYMENT_FAILED;
        this.failReason = reason;
        this.failedAt = now;
        this.updatedAt = now;
    }

    public void cancel() {
        validateCurrentStatus(OrderStatus.CREATED);
        this.status = OrderStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        validateCurrentStatus(OrderStatus.PAID);
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateCurrentStatus(OrderStatus expected) {
        if (this.status != expected) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    public void markPaymentApproved() {
        validateCurrentStatus(OrderStatus.PAYMENT_PENDING);
        this.status = OrderStatus.PAYMENT_APPROVED;
        this.updatedAt = LocalDateTime.now();
    }
}

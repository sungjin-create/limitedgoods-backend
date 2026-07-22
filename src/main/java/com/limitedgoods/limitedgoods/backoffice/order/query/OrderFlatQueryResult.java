package com.limitedgoods.limitedgoods.backoffice.order.query;

import com.limitedgoods.limitedgoods.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderFlatQueryResult(
    Long orderId,
    String email,
    long totalPrice,
    OrderStatus status,
    LocalDateTime createdAt,

    Long productId,
    String productName,
    int quantity,
    int price
) {

}

package com.limitedgoods.limitedgoods.order.application.create.dto;

import com.limitedgoods.limitedgoods.order.entity.OrderItem;

import java.util.List;
import java.util.Set;

public record OrderStockReservationResult(
        long totalPrice,
        List<OrderItem> orderItems,
        Set<Long> productIds
) {
}
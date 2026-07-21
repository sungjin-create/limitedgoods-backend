package com.limitedgoods.limitedgoods.order.dto.response;

public record OrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        int unitPrice,
        long lineTotalPrice
) {
}

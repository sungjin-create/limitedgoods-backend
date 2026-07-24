package com.limitedgoods.limitedgoods.event.payload.order;

public record OrderPaidItem(
        Long productId,
        int quantity,
        int unitPrice
) {
}

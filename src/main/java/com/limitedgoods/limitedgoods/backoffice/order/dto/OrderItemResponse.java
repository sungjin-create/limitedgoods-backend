package com.limitedgoods.limitedgoods.backoffice.order.dto;

public record OrderItemResponse (
    long productId,
    String productName,
    long quantity,
    long price
) {

}

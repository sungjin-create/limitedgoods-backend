package com.limitedgoods.limitedgoods.backoffice.product.dto;

public record ProductOrderSummaryQueryResult (
    Integer availableStock,
    Long orderPendingStock,
    Long paymentPendingStock
) {

}

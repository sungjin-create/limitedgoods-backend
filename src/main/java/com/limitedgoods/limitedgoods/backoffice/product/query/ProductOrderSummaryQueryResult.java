package com.limitedgoods.limitedgoods.backoffice.product.query;

public record ProductOrderSummaryQueryResult (
    Integer availableStock,
    Long orderPendingStock,
    Long paymentPendingStock
) {

}

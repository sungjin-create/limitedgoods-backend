package com.limitedgoods.limitedgoods.backoffice.product.dto;

import java.time.LocalDateTime;


public record ProductStockOverViewResponse (
    Integer availableStock,
    Long orderPendingStock,
    Long paymentPendingStock,
    LocalDateTime snapshotAt
){
    public static ProductStockOverViewResponse from (ProductOrderSummaryQueryResult result){
        return new ProductStockOverViewResponse(
                result.availableStock(),
                result.orderPendingStock(),
                result.paymentPendingStock(),
                LocalDateTime.now()
        );
    }
}

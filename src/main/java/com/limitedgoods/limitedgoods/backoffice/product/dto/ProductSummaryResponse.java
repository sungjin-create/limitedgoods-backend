package com.limitedgoods.limitedgoods.backoffice.product.dto;

public record ProductSummaryResponse (
        Long totalCount,
        Long lowStockCount,
        Long soldOutCount
){

}

package com.limitedgoods.limitedgoods.backoffice.product.dto.response;

public record ProductSummaryResponse (
        Long totalCount,
        Long lowStockCount,
        Long soldOutCount
){

}

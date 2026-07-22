package com.limitedgoods.limitedgoods.backoffice.product.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductListResponse (
        ProductSummaryResponse summary,
        List<ProductResponse> products
){

}

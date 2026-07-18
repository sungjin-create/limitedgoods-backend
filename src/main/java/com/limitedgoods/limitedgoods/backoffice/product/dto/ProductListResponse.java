package com.limitedgoods.limitedgoods.backoffice.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
public record ProductListResponse (
        ProductSummaryResponse summary,
        List<ProductResponse> products
){

}

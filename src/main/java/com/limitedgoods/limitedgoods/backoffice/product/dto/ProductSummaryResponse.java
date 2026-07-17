package com.limitedgoods.limitedgoods.backoffice.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductSummaryResponse {
    private long totalCount;
    private long lowStock;
    private long soldOut;
}

package com.limitedgoods.limitedgoods.backoffice.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class BackofficeProductResponse {

    private BackofficeProductSummaryResponse summary;
    private List<BackofficeProductsResponse> products;

}

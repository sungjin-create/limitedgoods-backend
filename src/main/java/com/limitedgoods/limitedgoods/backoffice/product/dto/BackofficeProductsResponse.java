package com.limitedgoods.limitedgoods.backoffice.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BackofficeProductsResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private int stock;

}

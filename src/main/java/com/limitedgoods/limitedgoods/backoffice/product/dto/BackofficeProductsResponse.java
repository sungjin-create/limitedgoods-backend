package com.limitedgoods.limitedgoods.backoffice.product.dto;

import com.limitedgoods.limitedgoods.product.entity.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class BackofficeProductsResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private int initialStock;
    private int stock;
    private int soldCount;
    private Integer maxPurchaseQuantity;
    private ProductType type;
    private boolean visible;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

}

package com.limitedgoods.limitedgoods.backoffice.product.dto;

import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.entity.ProductType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private int price;
    private int initialStock;
    private int stock;
    private int soldCount;
    private Integer maxPurchaseQuantity;
    private ProductType type;
    private ProductStatus status;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

}

package com.limitedgoods.limitedgoods.product.dto;

import com.limitedgoods.limitedgoods.product.entity.ProductType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private int price;
    private int stock;
    private Integer maxPurchaseQuantity;
    private ProductType type;
    private boolean visible;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
}

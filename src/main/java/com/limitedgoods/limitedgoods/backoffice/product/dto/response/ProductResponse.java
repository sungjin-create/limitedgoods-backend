package com.limitedgoods.limitedgoods.backoffice.product.dto.response;

import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.entity.ProductType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
public record ProductResponse (
    Long id,
    String name,
    String description,
    int price,
    int initialStock,
    int stock,
    int soldCount,
    Integer maxPurchaseQuantity,
    ProductType type,
    ProductStatus status,
    LocalDateTime saleStartAt,
    LocalDateTime saleEndAt
){

}

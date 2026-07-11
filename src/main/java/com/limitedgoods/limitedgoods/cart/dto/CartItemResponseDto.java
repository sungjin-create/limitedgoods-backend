package com.limitedgoods.limitedgoods.cart.dto;

import com.limitedgoods.limitedgoods.cart.entity.Cart;
import com.limitedgoods.limitedgoods.product.entity.Product;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CartItemResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private int price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

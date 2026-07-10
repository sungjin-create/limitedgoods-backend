package com.limitedgoods.limitedgoods.cart.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CartItemResponseDto {
    private Long id;
    private Long cartId;
    private Long productId;
    private int quantity;
    private int price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

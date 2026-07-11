package com.limitedgoods.limitedgoods.cart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequestDto {

    private Long productId;
    private int quantity;
}

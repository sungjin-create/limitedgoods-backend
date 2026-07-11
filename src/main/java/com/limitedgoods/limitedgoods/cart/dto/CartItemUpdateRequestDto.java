package com.limitedgoods.limitedgoods.cart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemUpdateRequestDto {
    private Long cartItemId;
    private int quantity;
}

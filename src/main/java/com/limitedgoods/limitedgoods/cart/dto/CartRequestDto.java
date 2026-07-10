package com.limitedgoods.limitedgoods.cart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartRequestDto {

    private Long productId;
    private int quantity;
}

package com.limitedgoods.limitedgoods.cart.dto;

import com.limitedgoods.limitedgoods.product.entity.Product;

import java.util.List;

public class CartResponseDto {

    private Long id;

    private Long userId;

    private List<Product> productList;

    private int totalPrice;

}

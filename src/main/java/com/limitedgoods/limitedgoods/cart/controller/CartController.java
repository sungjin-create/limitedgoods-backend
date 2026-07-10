package com.limitedgoods.limitedgoods.cart.controller;

import com.limitedgoods.limitedgoods.cart.dto.CartItemResponseDto;
import com.limitedgoods.limitedgoods.cart.dto.CartRequestDto;
import com.limitedgoods.limitedgoods.cart.dto.CartResponseDto;
import com.limitedgoods.limitedgoods.cart.entity.CartItem;
import com.limitedgoods.limitedgoods.cart.service.CartService;
import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> getCart(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        List<CartItemResponseDto> cartItemList =  cartService.getCartItemList(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(cartItemList));
    }

    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse<CartItemResponseDto>> addToCart(
            @RequestBody CartRequestDto cartRequestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        Long userId = customUserDetails.getUserId();
        Long productId = cartRequestDto.getProductId();
        int quantity = cartRequestDto.getQuantity();
        CartItemResponseDto cartItemResponseDto = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cartItemResponseDto));
    }
}

package com.limitedgoods.limitedgoods.cart.controller;

import com.limitedgoods.limitedgoods.cart.dto.*;
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
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CartItemRequestDto cartItemRequestDto
    ){
        Long userId = customUserDetails.getUserId();
        Long productId = cartItemRequestDto.getProductId();
        int quantity = cartItemRequestDto.getQuantity();
        CartItemResponseDto cartItemResponseDto = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cartItemResponseDto));
    }

    @PostMapping("/item/update")
    public ResponseEntity<ApiResponse<CartItemResponseDto>> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CartItemUpdateRequestDto cartItemUpdateRequestDto
    ){
        Long userId = customUserDetails.getUserId();
        Long cartItemId = cartItemUpdateRequestDto.getCartItemId();
        int quantity = cartItemUpdateRequestDto.getQuantity();
        cartService.updateCartItem(userId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/item")
    public ResponseEntity<ApiResponse> deleteCartItem(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam Long cartItemId){
        cartService.deleteCartItem(cartItemId, customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}

package com.limitedgoods.limitedgoods.cart.service;

import com.limitedgoods.limitedgoods.cart.dto.CartItemResponseDto;
import com.limitedgoods.limitedgoods.cart.dto.CartResponseDto;
import com.limitedgoods.limitedgoods.cart.entity.Cart;
import com.limitedgoods.limitedgoods.cart.entity.CartItem;
import com.limitedgoods.limitedgoods.cart.repository.CartItemRepository;
import com.limitedgoods.limitedgoods.cart.repository.CartRepository;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public List<CartItemResponseDto> getCartItemList(Long userId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> createCart(user));

        return cartItemRepository.findCartItemByCart(cart)
                .stream()
                .map(this::toCartItemResponseDto)
                .toList();
    }

    @Transactional
    public CartItemResponseDto addToCart(Long userId, Long productId, int quantity){
        //기존에 사용자 아이디로 카트가 만들어져있으면 해당 카트에 insert
        //기존 카트가 없다면 새로운 카트를 만든후 insert
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(()->createCart(user));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        if(product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int price = product.getPrice() * quantity;

        CartItem cartItem = cartItemRepository.save(
                CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .price(price)
                        .quantity(quantity)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        return toCartItemResponseDto(cartItem);
    }

    private Cart createCart(User user){
        return cartRepository.save(Cart.builder()
                        .user(user)
                        .created_at(LocalDateTime.now())
                        .updated_at(LocalDateTime.now())
                        .build());
    }

    private CartItemResponseDto toCartItemResponseDto(CartItem cartItem){

        return CartItemResponseDto.builder()
                .id(cartItem.getId())
                .cartId(cartItem.getCart().getId())
                .price(cartItem.getPrice())
                .productId(cartItem.getProduct().getId())
                .quantity(cartItem.getQuantity())
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}

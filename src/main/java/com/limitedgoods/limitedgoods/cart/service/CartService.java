package com.limitedgoods.limitedgoods.cart.service;

import com.limitedgoods.limitedgoods.cart.dto.CartItemResponseDto;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(()->createCart(user));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        if (cartItemRepository.existsCartItemByProduct(product)) {
            throw new BusinessException(ErrorCode.CART_ITEM_ALREADY_ADD);
        }

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

    @Transactional
    public void updateCartItem(Long userId, Long cartItemId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(()->createCart(user));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        Product product = productRepository.findById(cartItem.getProduct().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        if(product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int price = product.getPrice() * quantity;

        cartItem.updateQuantityAndPrice(quantity, price);
    }

    @Transactional
    public void deleteCartItem(Long cartItemId, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        cartRepository.findByUser(user)
                .orElseThrow(()-> new BusinessException(ErrorCode.CART_NOT_FOUND));

        cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.deleteById(cartItemId);

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
                .cart(cartItem.getCart())
                .price(cartItem.getPrice())
                .product(cartItem.getProduct())
                .quantity(cartItem.getQuantity())
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}

package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.orderitem.entity.OrderItem;
import com.limitedgoods.limitedgoods.orderitem.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisStockService redisStockService;

    @Transactional
    public OrderResponseDto redisSaveOrder(Long userId, OrderRequestDto dto) {
        Long productId = dto.getProductId();
        int quantity = dto.getQuantity();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        // DB 재고도 동기화 (정합성 유지)
        int updated = productRepository.decreaseStock(productId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int totalPrice = quantity * product.getPrice();

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();

        orderItemRepository.save(orderItem);

        return OrderResponseDto.builder()
                .id(savedOrder.getId())
                .userId(user.getId())
                .status(savedOrder.getStatus().name())
                .totalPrice(savedOrder.getTotalPrice())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }


    @Transactional
    public OrderResponseDto saveOrderWithPessimisticLock(Long userId, OrderRequestDto dto) {
        Long productId = dto.getProductId();
        int quantity = dto.getQuantity();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비관적 락으로 조회 (FOR UPDATE)
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        // 엔티티에서 직접 차감 (dirty checking으로 DB 반영)
        product.decreaseStock(quantity);

        int totalPrice = quantity * product.getPrice();

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();

        orderItemRepository.save(orderItem);

        return OrderResponseDto.builder()
                .id(savedOrder.getId())
                .userId(user.getId())
                .status(savedOrder.getStatus().name())
                .totalPrice(savedOrder.getTotalPrice())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

}

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisStockService redisStockService;

    public OrderResponseDto createOrder(Long userId, OrderRequestDto dto) {
        Long productId = dto.getProductId();
        int quantity = dto.getQuantity();

        // 1. Redis 원자 차감 (락 없이 안전하게)
        redisStockService.decreaseStock(productId, quantity);

        try {
            // 2. DB 주문 저장 (트랜잭션)
            return saveOrder(userId, dto);
        } catch (Exception e) {
            // 3. DB 저장 실패 시 Redis 재고 복구 (보상 트랜잭션)
            redisStockService.increaseStock(productId, quantity);
            throw e;
        }
    }

    @Transactional
    public OrderResponseDto saveOrder(Long userId, OrderRequestDto dto) {
        Long productId = dto.getProductId();
        int quantity = dto.getQuantity();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        // DB 재고도 동기화 (정합성 유지)
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

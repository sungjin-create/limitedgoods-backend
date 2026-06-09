package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.OrderPaymentInfo;
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
import java.util.List;


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

    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto dto, long reservationSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        int totalPrice = product.getPrice() * dto.getQuantity();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(reservationSeconds);

        Order order = Order.create(user, totalPrice, expiresAt);
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(savedOrder)
                .product(product)
                .quantity(dto.getQuantity())
                .price(product.getPrice())
                .build();

        orderItemRepository.save(orderItem);

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderPaymentInfo startPayment(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        order.markPaymentPending();

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderPaymentInfo(
                order.getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                order.getTotalPrice()
        );
    }

    @Transactional
    public OrderResponseDto completePayment(Long userId, Long orderId, Long productId, int quantity) {
        int updated = productRepository.decreaseStock(productId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        Order order = getOrder(orderId, userId);
        order.markPaid();

        return toResponse(order);
    }

    @Transactional
    public void failPayment(Long userId, Long orderId, String reason) {
        Order order = getOrder(orderId, userId);
        order.markPaymentFailed(reason);
    }

    private Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    private OrderResponseDto toResponse(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional
    public void expireOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CREATED
                && order.getStatus() != OrderStatus.PAYMENT_PENDING
                && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            return;
        }

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.markExpired();
        redisStockService.increaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds() {
        return orderRepository.findByStatusInAndExpiresAtBefore(
                        List.of(
                                OrderStatus.CREATED,
                                OrderStatus.PAYMENT_PENDING,
                                OrderStatus.PAYMENT_FAILED
                        ),
                        LocalDateTime.now()
                )
                .stream()
                .map(Order::getId)
                .toList();
    }

}

package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.payload.OrderCanceledEvent;
import com.limitedgoods.limitedgoods.event.payload.OrderExpiredEvent;
import com.limitedgoods.limitedgoods.event.payload.OrderPaidEvent;
import com.limitedgoods.limitedgoods.order.dto.*;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.order.reservation.RedisReservationService;
import com.limitedgoods.limitedgoods.stock.service.RedisStockService;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisStockService redisStockService;
    private final RedisReservationService redisReservationService;
    private final MeterRegistry meterRegistry;
    private final OutboxEventService outboxEventService;

    @PersistenceContext
    EntityManager em;

    @Transactional(readOnly = true)
    public List<OrderDetailResponseDto> getMyOrders(Long userId) {
        return orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> {
                    OrderItem item = orderItemRepository.findByOrderId(order.getId()).orElse(null);
                    return toDetailResponse(order, item);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrderDetail(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        OrderItem item = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return toDetailResponse(order, item);
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

    @Transactional(readOnly = true)
    public OrderPaymentInfo getPaymentInfo(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderPaymentInfo(
                order.getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }

    @Transactional
    public OrderPaymentInfo startPayment(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);
        order.markPaymentPending();

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderPaymentInfo(
                order.getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }

    @Transactional
    public void markPaymentApproved(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.PAYMENT_APPROVED
                || order.getStatus() == OrderStatus.PAID) {
            return;
        }

        order.markPaymentApproved();
    }

    @Transactional
    public OrderResponseDto finalizeApprovedPayment(Long userId, Long orderId) {

        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.PAID) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PAYMENT_APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        int updated = productRepository.decreaseStock(
                orderItem.getProduct().getId(),
                orderItem.getQuantity()
        );

        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        order.markPaid();

        outboxEventService.save(
                OutboxEventType.ORDER_PAID,
                "ORDER",
                order.getId(),
                new OrderPaidEvent(
                        order.getId(),
                        userId,
                        order.getTotalPrice(),
                        LocalDateTime.now()
                )
        );

        return toResponse(order);
    }

    @Transactional
    public void expireOrder(Long orderId) {
        int updated = orderRepository.expireIfActive(
                orderId,
                OrderStatus.EXPIRED,
                List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_FAILED),
                LocalDateTime.now()
        );

        if (updated == 0) {
            return;
        }

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        redisStockService.increaseStock(orderItem.getProduct().getId(), orderItem.getQuantity());
        redisReservationService.deleteReservation(orderId);
        outboxEventService.save(
                OutboxEventType.ORDER_EXPIRED,
                "ORDER",
                orderId,
                new OrderExpiredEvent(
                        orderId,
                        orderItem.getProduct().getId(),
                        orderItem.getQuantity(),
                        LocalDateTime.now()
                )
        );
        meterRegistry.counter("order.expired").increment();
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredOrderIds() {
        return orderRepository.findByStatusInAndExpiresAtBefore(
                        List.of(
                                OrderStatus.CREATED,
                                OrderStatus.PAYMENT_FAILED
                        ),
                        LocalDateTime.now()
                )
                .stream()
                .map(Order::getId)
                .toList();
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

    private OrderDetailResponseDto toDetailResponse(Order order, OrderItem item) {
        return OrderDetailResponseDto.builder()
                .orderId(order.getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .expiresAt(order.getExpiresAt())
                .productId(item != null ? item.getProduct().getId() : null)
                .productName(item != null ? item.getProduct().getName() : null)
                .quantity(item != null ? item.getQuantity() : 0)
                .unitPrice(item != null ? item.getPrice() : 0)
                .build();
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

    private Order getOrderForUpdate(Long orderId, Long userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    @Transactional
    public OrderResponseDto cancelPaidOrder(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        productRepository.increaseStock(
                orderItem.getProduct().getId(),
                orderItem.getQuantity()
        );

        redisStockService.increaseStock(
                orderItem.getProduct().getId(),
                orderItem.getQuantity()
        );

        order.cancelPaidOrder();

        return toResponse(order);
    }

    @Transactional
    public OrderPaymentInfo requestCancel(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        if (order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
            return getPaymentInfo(userId, orderId);
        }

        if (order.getStatus() == OrderStatus.CANCEL_FAILED) {
            return getPaymentInfo(userId, orderId);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        order.requestCancel();

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderPaymentInfo(
                order.getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }

    @Transactional
    public OrderResponseDto completeRefund(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED
                && order.getStatus() != OrderStatus.CANCEL_FAILED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        OrderItem orderItem = orderItemRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        productRepository.increaseStock(
                orderItem.getProduct().getId(),
                orderItem.getQuantity()
        );

        redisStockService.increaseStock(
                orderItem.getProduct().getId(),
                orderItem.getQuantity()
        );

        order.markRefunded();

        outboxEventService.save(
                OutboxEventType.ORDER_CANCELED,
                "ORDER",
                order.getId(),
                new OrderCanceledEvent(
                        order.getId(),
                        userId,
                        orderItem.getProduct().getId(),
                        orderItem.getQuantity(),
                        LocalDateTime.now()
                )
        );

        return toResponse(order);
    }

    @Transactional
    public void failRefund(Long userId, Long orderId, String reason) {
        Order order = getOrderForUpdate(orderId, userId);

        if (order.getStatus() == OrderStatus.REFUNDED) {
            return;
        }

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.markCancelFailed(reason);
    }

}

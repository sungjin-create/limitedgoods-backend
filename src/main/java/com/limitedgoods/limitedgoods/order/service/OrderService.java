package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.event.payload.order.OrderCanceledEvent;
import com.limitedgoods.limitedgoods.event.payload.order.OrderExpiredEvent;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidEvent;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidItem;
import com.limitedgoods.limitedgoods.order.dto.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import com.limitedgoods.limitedgoods.order.dto.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
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
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MeterRegistry meterRegistry;
    private final OutboxEventService outboxEventService;
    private final SoldOutCacheService soldOutCacheService;

    @Transactional(readOnly = true)
    public List<OrderDetailResponseDto> getMyOrders(Long userId) {
        return orderRepository.findMyOrderDetails(userId);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrderDetail(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        return toDetailResponse(order, items.get(0));
    }

    @Transactional
    public OrderResponseDto createOrder(Long userId,
                                        List<OrderItemsListDto> items,
                                        long reservationSeconds,
                                        String checkoutToken,
                                        String requestFingerprint) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<Order> existing = findByCheckoutToken(userId, checkoutToken);

        if (existing.isPresent()) {
            Order order = existing.get();

            validateRequestFingerprint(order, requestFingerprint);

            return toResponse(order);
        }


        long totalPrice = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        List<OrderItemsListDto> sortedItems = items.stream()
                .sorted(Comparator.comparing(OrderItemsListDto::getProductId))
                .toList();

        Set<Long> productIds = new HashSet<>();

        //기존 주문 취소 및 기존 주문의 재고 복구
        cancelActivePendingOrder(userId);

        for (OrderItemsListDto item : sortedItems) {
            Product product =productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

            int updated = productRepository.decreaseStockIfPurchasable(
                    product.getId(),
                    item.getQuantity(),
                    ProductStatus.ACTIVE,
                    ProductStatus.SCHEDULED
            );

            if (updated == 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }

            productIds.add(product.getId());

            long lineTotalPrice = product.getPrice() * item.getQuantity();

            totalPrice += lineTotalPrice;

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .lineTotalPrice(lineTotalPrice)
                    .build());
        }

        List<Long> soldOutProductIds =
                productRepository.findSoldOutProductIds(productIds);
        soldOutProductIds.forEach(soldOutCacheService::markSoldOutAfterCommit);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(reservationSeconds);

        Order order = Order.create(user, totalPrice, expiresAt, checkoutToken, requestFingerprint);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderPaymentInfo getPaymentInfo(Long userId, Long orderId) {
        Order order = getOrder(orderId, userId);
        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
    }

    @Transactional
    public OrderPaymentInfo startPayment(Long userId, Long orderId) {
        Order order = getOrderForUpdate(orderId, userId);
        order.markPaymentPending(LocalDateTime.now());
        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
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

        order.markPaid();

        //product의 soldOut 재고 갯수 업데이트
        updateProductSoldCount(userId, orderId);

        outboxEventService.save(
                OutboxEventType.ORDER_PAID,
                "ORDER",
                order.getId(),
                new OrderPaidEvent(
                        order.getId(),
                        userId,
                        order.getUser().getEmail(),
                        order.getTotalPrice(),
                        LocalDateTime.now(),
                        orderItemRepository.findByOrderId(order.getId()).stream()
                                .map(item -> new OrderPaidItem(
                                        item.getProduct().getId(),
                                        item.getQuantity(),
                                        item.getPrice()
                                ))
                                .toList()
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

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProduct().getId();

            productRepository.increaseStock(
                    productId,
                    orderItem.getQuantity()
            );
            soldOutCacheService.clearSoldOutAfterCommit(productId);
        }

        outboxEventService.save(
                OutboxEventType.ORDER_EXPIRED,
                "ORDER",
                orderId,
                new OrderExpiredEvent(orderId, LocalDateTime.now())
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

    @Transactional(readOnly = true)
    public OrderResponseDto findIdempotentOrder(
            Long userId,
            String checkoutToken,
            String requestFingerprint
    ) {
        return findByCheckoutToken(userId, checkoutToken)
                .map(order -> {
                    validateRequestFingerprint(
                            order,
                            requestFingerprint
                    );

                    return toResponse(order);
                })
                .orElse(null);
    }

    private void validateRequestFingerprint(
            Order order,
            String requestFingerprint
    ) {
        if (!order.getRequestFingerprint()
                .equals(requestFingerprint)) {
            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_KEY_REUSED
            );
        }
    }

    private Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    private void cancelActivePendingOrder(Long userId) {
        List<Order> orderList = orderRepository.findActiveOrdersForUpdate(userId,
                List.of(OrderStatus.CREATED,
                        OrderStatus.PAYMENT_FAILED,
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.PAYMENT_APPROVED));

        for (Order order : orderList) {
            if (order.getStatus() == OrderStatus.PAYMENT_PENDING
                    || order.getStatus() == OrderStatus.PAYMENT_APPROVED) {
                throw new BusinessException(ErrorCode.ORDER_STARTING_PAYMENT);
            }

            // CREATED, PAYMENT_FAILED → 직접 처리 (expireOrder 호출 대신)
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

            for (OrderItem item : items) {
                productRepository.increaseStock(
                        item.getProduct().getId(),
                        item.getQuantity()
                );
                soldOutCacheService.clearSoldOutAfterCommit(item.getProduct().getId());
            }

            order.markExpired();
        }
    }

    private OrderDetailResponseDto toDetailResponse(Order order, OrderItem item) {
        return new OrderDetailResponseDto(order.getId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                item != null ? item.getProduct().getId() : null,
                item != null ? item.getProduct().getName() : null,
                item != null ? item.getQuantity() : 0,
                item != null ? item.getPrice() : 0
                );
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
        Order order = orderRepository.findByIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return order;
    }

    private void updateProductSoldCount(Long userId, Long orderId) {
        List<OrderItem> orderItemList = orderRepository.findOrderItemsByOrder(orderId, userId);

        for(OrderItem orderItem : orderItemList) {
            productRepository.increaseSoldCount(
                    orderItem.getProduct().getId(),
                    orderItem.getQuantity());
        }
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

        return new OrderPaymentInfo(order.getId(), order.getTotalPrice(), order.getStatus());
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

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        for (OrderItem orderItem : orderItems) {
            Long productId = orderItem.getProduct().getId();

            productRepository.increaseStock(
                    productId,
                    orderItem.getQuantity()
            );
            soldOutCacheService.clearSoldOutAfterCommit(productId);
        }

        order.markRefunded();

        outboxEventService.save(
                OutboxEventType.ORDER_CANCELED,
                "ORDER",
                order.getId(),
                new OrderCanceledEvent(order.getId(), userId, LocalDateTime.now())
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

    private Optional<Order> findByCheckoutToken(Long userId, String checkoutToken) {
        return orderRepository.findByUserIdAndCheckoutToken(userId,checkoutToken);
    }

}

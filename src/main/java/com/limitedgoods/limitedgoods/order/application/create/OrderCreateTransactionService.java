package com.limitedgoods.limitedgoods.order.application.create;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.application.mapper.OrderResponseMapper;
import com.limitedgoods.limitedgoods.order.dto.request.OrderItemsListDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class OrderCreateTransactionService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderResponseMapper orderResponseMapper;
    private final ProductSoldOutCacheService productSoldOutCacheService;
    private final OrderItemRepository orderItemRepository;


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

            return orderResponseMapper.toResponse(order);
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

            long lineTotalPrice = Math.multiplyExact(
                    (long) product.getPrice(),
                    item.getQuantity()
            );

            totalPrice = Math.addExact(
                    totalPrice,
                    lineTotalPrice
            );

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .lineTotalPrice(lineTotalPrice)
                    .build());
        }

        List<Long> soldOutProductIds =
                productRepository.findSoldOutProductIds(productIds);
        soldOutProductIds.forEach(productSoldOutCacheService::markSoldOutAfterCommit);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(reservationSeconds);

        Order order = Order.create(user, totalPrice, expiresAt, checkoutToken, requestFingerprint);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);

        return orderResponseMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto findIdempotentOrder(
            Long userId,
            String checkoutToken,
            String requestFingerprint
    ) {
        Optional<Order> optionalOrder =
                findByCheckoutToken(userId, checkoutToken);

        if (optionalOrder.isPresent()) {

            Order order = optionalOrder.get();

            validateRequestFingerprint(
                    order,
                    requestFingerprint
            );

            return orderResponseMapper.toResponse(order);
        }

        return null;
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
                productSoldOutCacheService.clearSoldOutAfterCommit(item.getProduct().getId());
            }

            order.markExpired();
        }
    }

    private void validateRequestFingerprint(
            Order order,
            String requestFingerprint
    ) {
        if (!order.getRequestFingerprint()
                .equals(requestFingerprint)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
    }

    private Optional<Order> findByCheckoutToken(Long userId, String checkoutToken) {
        return orderRepository.findByUserIdAndCheckoutToken(userId,checkoutToken);
    }
}

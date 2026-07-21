package com.limitedgoods.limitedgoods.order.application.expiration;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.event.payload.order.OrderExpiredEvent;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductSoldOutCacheService productSoldOutCacheService;
    private final OutboxEventService outboxEventService;


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

            productRepository.increaseStock(productId, orderItem.getQuantity());
            productSoldOutCacheService.clearSoldOutAfterCommit(productId);
        }

        outboxEventService.save(
                OutboxEventType.ORDER_EXPIRED,
                "ORDER",
                orderId,
                new OrderExpiredEvent(orderId, LocalDateTime.now())
        );
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
}

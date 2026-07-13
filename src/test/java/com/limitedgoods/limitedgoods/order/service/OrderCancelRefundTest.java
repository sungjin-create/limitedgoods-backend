package com.limitedgoods.limitedgoods.order.service;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.order.dto.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.support.OrderTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancelRefundTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRepository productRepository;
    @Mock OutboxEventService outboxEventService;
    @Mock SoldOutCacheService soldOutCacheService;

    @InjectMocks OrderService orderService;

    @Test
    @DisplayName("PAID 주문은 취소 요청 시 CANCEL_REQUESTED 상태가 된다")
    void requestCancel_success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = OrderTestFactory.paidOrder(orderId, userId);
        OrderItem orderItem = OrderTestFactory.orderItem(order, 1L, 1);

        when(orderRepository.findByIdForUpdate(orderId, userId))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId))
                .thenReturn(List.of(orderItem));

        // when
        OrderPaymentInfo result =
                orderService.requestCancel(userId, orderId);

        // then
        assertThat(result.orderStatus())
                .isEqualTo(OrderStatus.CANCEL_REQUESTED);
    }

    @Test
    @DisplayName("환불 성공 시 REFUNDED 상태가 되고 재고가 복구된다")
    void completeRefund_success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = OrderTestFactory.cancelRequestedOrder(orderId, userId);
        OrderItem orderItem = OrderTestFactory.orderItem(order, 1L, 2);

        when(orderRepository.findByIdForUpdate(orderId, userId))
                .thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId))
                .thenReturn(List.of(orderItem));

        // when
        OrderResponseDto response =
                orderService.completeRefund(userId, orderId);

        // then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.REFUNDED.name());
        verify(productRepository).increaseStock(1L, 2);
        verify(soldOutCacheService).clearSoldOutAfterCommit(1L);
    }

    @Test
    @DisplayName("환불 실패 시 CANCEL_FAILED 상태가 된다")
    void failRefund_success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = OrderTestFactory.cancelRequestedOrder(orderId, userId);

        when(orderRepository.findByIdForUpdate(orderId, userId))
                .thenReturn(Optional.of(order));

        // when
        orderService.failRefund(userId, orderId, "PG 환불 실패");

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCEL_FAILED);
        assertThat(order.getCancelFailReason()).isEqualTo("PG 환불 실패");
    }
}

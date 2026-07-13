package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    OutboxEventService outboxEventService;

    @InjectMocks
    OrderService orderService;

    @Test
    @DisplayName("결제 승인 상태의 주문을 결제 완료 처리한다")
    void finalizeApprovedPayment_success() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = OrderTestFactory.paymentApprovedOrder(orderId, userId);
        when(orderRepository.findByIdForUpdate(orderId, userId))
                .thenReturn(Optional.of(order));

        // when
        OrderResponseDto response =
                orderService.finalizeApprovedPayment(userId, orderId);

        // then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID.name());
        verify(productRepository, never()).decreaseStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("이미 결제 완료된 주문은 재요청해도 그대로 반환한다")
    void finalizeApprovedPayment_alreadyPaid_idempotent() {
        // given
        Long userId = 1L;
        Long orderId = 1L;

        Order order = OrderTestFactory.paidOrder(orderId, userId);

        when(orderRepository.findByIdForUpdate(orderId, userId))
                .thenReturn(Optional.of(order));

        // when
        OrderResponseDto response =
                orderService.finalizeApprovedPayment(userId, orderId);

        // then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID.name());
        verify(productRepository, never()).decreaseStock(anyLong(), anyInt());
    }
}

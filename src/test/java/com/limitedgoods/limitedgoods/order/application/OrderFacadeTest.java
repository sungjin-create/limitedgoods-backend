package com.limitedgoods.limitedgoods.order.application;

import com.limitedgoods.limitedgoods.cart.service.CartService;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.idempotency.service.PaymentIdempotencyService;
import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import com.limitedgoods.limitedgoods.order.dto.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.service.OrderRateLimitService;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.order.service.SoldOutCacheService;
import com.limitedgoods.limitedgoods.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.payment.dto.PaymentResult;
import com.limitedgoods.limitedgoods.payment.service.PaymentFailedException;
import com.limitedgoods.limitedgoods.payment.service.PaymentService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    OrderService orderService;

    @Mock
    PaymentService paymentService;

    @Mock
    PaymentIdempotencyService paymentIdempotencyService;

    @Mock
    CartService cartService;

    @Mock
    SoldOutCacheService soldOutCacheService;

    @Mock
    OrderRateLimitService orderRateLimitService;

    OrderFacade orderFacade;

    @BeforeEach
    void setUp() {
        orderFacade = new OrderFacade(
                orderService,
                paymentService,
                paymentIdempotencyService,
                cartService,
                new SimpleMeterRegistry(),
                soldOutCacheService,
                orderRateLimitService
        );
    }

    @Test
    @DisplayName("createOrder success")
    void createOrder_success() {
        Long userId = 1L;
        Long productId = 10L;
        String checkoutToken = "checkout-token";
        OrderRequestDto request = orderRequest(checkoutToken, productId, 1);
        OrderResponseDto created = orderResponse(100L, userId, OrderStatus.CREATED);

        when(orderService.findActiveOrderByCheckoutToken(userId, checkoutToken))
                .thenReturn(null);
        when(orderRateLimitService.allow(userId, productId))
                .thenReturn(true);
        when(soldOutCacheService.isSoldOut(productId))
                .thenReturn(false);
        when(orderService.createOrder(eq(userId), eq(request.getItems()), eq(300L), eq(checkoutToken)))
                .thenReturn(created);

        OrderResponseDto response = orderFacade.createOrder(userId, request);

        assertThat(response).isSameAs(created);

        InOrder inOrder = inOrder(orderService, orderRateLimitService, soldOutCacheService);
        inOrder.verify(orderService).findActiveOrderByCheckoutToken(userId, checkoutToken);
        inOrder.verify(orderRateLimitService).allow(userId, productId);
        inOrder.verify(soldOutCacheService).isSoldOut(productId);
        inOrder.verify(orderService).cancelActivePendingOrder(userId);
        inOrder.verify(orderService).createOrder(userId, request.getItems(), 300L, checkoutToken);
    }

    @Test
    @DisplayName("createOrder returns existing active order for same checkout token")
    void createOrder_existingActiveOrder_returnsExistingOrder() {
        Long userId = 1L;
        Long productId = 10L;
        String checkoutToken = "checkout-token";
        OrderRequestDto request = orderRequest(checkoutToken, productId, 1);
        OrderResponseDto existing = orderResponse(100L, userId, OrderStatus.CREATED);

        when(orderService.findActiveOrderByCheckoutToken(userId, checkoutToken))
                .thenReturn(existing);

        OrderResponseDto response = orderFacade.createOrder(userId, request);

        assertThat(response).isSameAs(existing);
        verify(orderRateLimitService, never()).allow(any(), any());
        verify(soldOutCacheService, never()).isSoldOut(any());
        verify(orderService, never()).cancelActivePendingOrder(any());
        verify(orderService, never()).createOrder(any(), any(), eq(300L), any());
    }

    @Test
    @DisplayName("createOrder rejects rate limited request")
    void createOrder_rateLimited_throwsException() {
        Long userId = 1L;
        Long productId = 10L;
        String checkoutToken = "checkout-token";
        OrderRequestDto request = orderRequest(checkoutToken, productId, 1);

        when(orderService.findActiveOrderByCheckoutToken(userId, checkoutToken))
                .thenReturn(null);
        when(orderRateLimitService.allow(userId, productId))
                .thenReturn(false);

        assertThatThrownBy(() -> orderFacade.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOO_MANY_ORDER_REQUESTS);

        verify(soldOutCacheService, never()).isSoldOut(any());
        verify(orderService, never()).cancelActivePendingOrder(any());
        verify(orderService, never()).createOrder(any(), any(), eq(300L), any());
    }

    @Test
    @DisplayName("createOrder rejects sold out cache hit")
    void createOrder_soldOutCache_throwsException() {
        Long userId = 1L;
        Long productId = 10L;
        String checkoutToken = "checkout-token";
        OrderRequestDto request = orderRequest(checkoutToken, productId, 1);

        when(orderService.findActiveOrderByCheckoutToken(userId, checkoutToken))
                .thenReturn(null);
        when(orderRateLimitService.allow(userId, productId))
                .thenReturn(true);
        when(soldOutCacheService.isSoldOut(productId))
                .thenReturn(true);

        assertThatThrownBy(() -> orderFacade.createOrder(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        verify(orderService, never()).cancelActivePendingOrder(any());
        verify(orderService, never()).createOrder(any(), any(), eq(300L), any());
    }

    @Test
    @DisplayName("payOrder success")
    void payOrder_success() {
        Long userId = 1L;
        Long orderId = 100L;
        String idempotencyKey = "idem-key";
        PaymentRequestDto request = paymentRequest();
        OrderResponseDto paid = orderResponse(orderId, userId, OrderStatus.PAID);

        when(paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey))
                .thenReturn(null);
        when(paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey))
                .thenReturn(true);
        when(orderService.getPaymentInfo(userId, orderId))
                .thenReturn(new OrderPaymentInfo(orderId, 10000, OrderStatus.CREATED));
        when(orderService.startPayment(userId, orderId))
                .thenReturn(new OrderPaymentInfo(orderId, 10000, OrderStatus.PAYMENT_PENDING));
        when(paymentService.pay(orderId, 10000, request))
                .thenReturn(new PaymentResult("tx-1", LocalDateTime.now()));
        when(orderService.finalizeApprovedPayment(userId, orderId))
                .thenReturn(paid);

        OrderResponseDto response = orderFacade.payOrder(userId, orderId, request, idempotencyKey);

        assertThat(response).isSameAs(paid);

        InOrder inOrder = inOrder(paymentIdempotencyService, orderService, paymentService, cartService);
        inOrder.verify(paymentIdempotencyService).getSavedResponse(userId, orderId, idempotencyKey);
        inOrder.verify(paymentIdempotencyService).acquireLock(userId, orderId, idempotencyKey);
        inOrder.verify(orderService).getPaymentInfo(userId, orderId);
        inOrder.verify(orderService).startPayment(userId, orderId);
        inOrder.verify(paymentService).pay(orderId, 10000, request);
        inOrder.verify(orderService).markPaymentApproved(userId, orderId);
        inOrder.verify(orderService).finalizeApprovedPayment(userId, orderId);
        inOrder.verify(paymentIdempotencyService).saveResponse(userId, orderId, idempotencyKey, paid);
        inOrder.verify(cartService).clearCart(userId);
        inOrder.verify(paymentIdempotencyService).releaseLock(userId, orderId, idempotencyKey);
    }

    @Test
    @DisplayName("payOrder returns saved idempotency response")
    void payOrder_savedIdempotencyResponse_returnsSavedResponse() {
        Long userId = 1L;
        Long orderId = 100L;
        String idempotencyKey = "idem-key";
        OrderResponseDto saved = orderResponse(orderId, userId, OrderStatus.PAID);

        when(paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey))
                .thenReturn(saved);

        OrderResponseDto response = orderFacade.payOrder(userId, orderId, paymentRequest(), idempotencyKey);

        assertThat(response).isSameAs(saved);
        verify(paymentIdempotencyService, never()).acquireLock(any(), any(), any());
        verify(paymentService, never()).pay(any(), any(Integer.class), any());
        verify(orderService, never()).startPayment(any(), any());
    }

    @Test
    @DisplayName("payOrder rejects duplicate in progress")
    void payOrder_duplicateInProgress_throwsException() {
        Long userId = 1L;
        Long orderId = 100L;
        String idempotencyKey = "idem-key";

        when(paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey))
                .thenReturn(null);
        when(paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey))
                .thenReturn(false);

        assertThatThrownBy(() -> orderFacade.payOrder(userId, orderId, paymentRequest(), idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_PAYMENT_REQUEST);

        verify(paymentService, never()).pay(any(), any(Integer.class), any());
        verify(orderService, never()).startPayment(any(), any());
        verify(paymentIdempotencyService, never()).releaseLock(any(), any(), any());
    }

    @Test
    @DisplayName("payOrder marks failed and releases lock when PG fails")
    void payOrder_paymentFailed_marksFailedAndReleasesLock() {
        Long userId = 1L;
        Long orderId = 100L;
        String idempotencyKey = "idem-key";
        PaymentRequestDto request = paymentRequest();

        when(paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey))
                .thenReturn(null);
        when(paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey))
                .thenReturn(true);
        when(orderService.getPaymentInfo(userId, orderId))
                .thenReturn(new OrderPaymentInfo(orderId, 10000, OrderStatus.CREATED));
        when(orderService.startPayment(userId, orderId))
                .thenReturn(new OrderPaymentInfo(orderId, 10000, OrderStatus.PAYMENT_PENDING));
        when(paymentService.pay(orderId, 10000, request))
                .thenThrow(new PaymentFailedException("PG failed"));

        assertThatThrownBy(() -> orderFacade.payOrder(userId, orderId, request, idempotencyKey))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_FAILED);

        verify(orderService).failPayment(userId, orderId, "PG failed");
        verify(orderService, never()).markPaymentApproved(any(), any());
        verify(orderService, never()).finalizeApprovedPayment(any(), any());
        verify(paymentIdempotencyService).releaseLock(userId, orderId, idempotencyKey);
    }

    @Test
    @DisplayName("payOrder finalizes already approved payment without PG call")
    void payOrder_alreadyApproved_finalizesWithoutPgCall() {
        Long userId = 1L;
        Long orderId = 100L;
        String idempotencyKey = "idem-key";
        OrderResponseDto paid = orderResponse(orderId, userId, OrderStatus.PAID);

        when(paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey))
                .thenReturn(null);
        when(paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey))
                .thenReturn(true);
        when(orderService.getPaymentInfo(userId, orderId))
                .thenReturn(new OrderPaymentInfo(orderId, 10000, OrderStatus.PAYMENT_APPROVED));
        when(orderService.finalizeApprovedPayment(userId, orderId))
                .thenReturn(paid);

        OrderResponseDto response = orderFacade.payOrder(userId, orderId, paymentRequest(), idempotencyKey);

        assertThat(response).isSameAs(paid);
        verify(orderService, never()).startPayment(any(), any());
        verify(paymentService, never()).pay(any(), any(Integer.class), any());
        verify(paymentIdempotencyService).saveResponse(userId, orderId, idempotencyKey, paid);
        verify(paymentIdempotencyService).releaseLock(userId, orderId, idempotencyKey);
    }

    private OrderRequestDto orderRequest(String checkoutToken, Long productId, int quantity) {
        return OrderRequestDto.builder()
                .checkoutToken(checkoutToken)
                .items(List.of(OrderItemsListDto.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build()))
                .build();
    }

    private PaymentRequestDto paymentRequest() {
        return new PaymentRequestDto();
    }

    private OrderResponseDto orderResponse(Long orderId, Long userId, OrderStatus status) {
        return OrderResponseDto.builder()
                .id(orderId)
                .userId(userId)
                .totalPrice(10000)
                .status(status.name())
                .createdAt(LocalDateTime.now())
                .build();
    }
}

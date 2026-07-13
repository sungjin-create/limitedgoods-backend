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
import com.limitedgoods.limitedgoods.payment.service.PaymentFailedException;
import com.limitedgoods.limitedgoods.payment.service.PaymentService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentIdempotencyService paymentIdempotencyService;
    private final CartService cartService;
    private final MeterRegistry  meterRegistry;
    private final SoldOutCacheService soldOutCacheService;
    private final OrderRateLimitService orderRateLimitService;

    private final long EXPIRED_SECONDS = 300;

    @Retryable(
            retryFor = { Exception.class },       // 재시도할 예외 종류
            maxAttempts = 3,                       // 최대 3회 시도
            backoff = @Backoff(delay = 1000)       // 재시도 간격 1초
    )
    public OrderResponseDto retryFinalizeApprovedPayment(Long userId, Long orderId){
        return orderService.finalizeApprovedPayment(userId, orderId);
    }

    @Recover
    public OrderResponseDto recoverFinalize(Exception e, Long userId, Long orderId){
        log.error("[결제 확정 최종 실패] 재시도 3회 모두 실패. orderId = {}, userId = {}", orderId, userId);
        throw new BusinessException(ErrorCode.PAYMENT_FAILED);
    }

    public OrderResponseDto createOrder(Long userId, OrderRequestDto request) {

        //유효한 요청인지 검사
        validateOrderRequest(request);

        String checkoutToken = request.getCheckoutToken();

        /*
            토큰이 같은 경우의 상태별 반환
            상태 : CREATED, PAYMENT_PENDING, PAYMENT_APPROVED, PAYMENT_FAILED -> 기존 주문 반환
            나머지 : 주문 진행
         */
        OrderResponseDto existing = orderService.findActiveOrderByCheckoutToken(userId, checkoutToken);
        if (existing != null) {
            return existing;
        }

        /*
         * 사용자별·상품별 요청 제한.
         *
         * Redis 장애 시 OrderRateLimitService는 true를 반환하여
         * DB가 최종 판단하도록 구현한다.
         */
        validateRateLimit(userId, request);

        /*
         * 최근 DB에서 품절이 확인된 상품을 빠르게 거절한다.
         *
         * Redis 장애 또는 캐시 미스 시 false를 반환하고
         * DB 조건부 업데이트에서 최종 판단한다.
         */
        validateSoldOutCache(request);

        /*
         * 1인 1주문 원칙
         * 사용자의 기존 CREATED/PAYMENT_FAILED 주문을 만료하고
         * 해당 주문이 확보했던 DB 재고를 복구한다.
         *
         * PAYMENT_PENDING/PAYMENT_APPROVED 주문이 있다면 예외를 발생시킨다.
         */
        orderService.cancelActivePendingOrder(userId);

        try {
            OrderResponseDto order = orderService.createOrder(userId, request.getItems(), EXPIRED_SECONDS, checkoutToken);
            meterRegistry.counter("order.created", "result", "success").increment();
            return order;
        } catch (RuntimeException e) {
            meterRegistry.counter("order.created", "result", "fail").increment();
            throw e;
        }
    }

    public OrderResponseDto payOrder(Long userId, Long orderId, PaymentRequestDto request, String idempotencyKey) {

        // 1. 이미 성공 처리된 요청이면 저장된 응답을 그대로 반환 (멱등성 캐시)
        OrderResponseDto saved = paymentIdempotencyService.getSavedResponse(userId, orderId, idempotencyKey);
        if (saved != null) {
            return saved;
        }

        // 2. 동일한 Idempotency-Key로 동시에 들어온 중복 요청 차단 (분산 락)
        boolean locked = paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey);
        if (!locked) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        try {
            // 3. 현재 주문 상태 조회
            //    - PAYMENT_APPROVED: 외부 결제는 이미 승인됐으나 내부 확정이 실패한 경우 → PG 재호출 없이 내부 확정만 재시도
            //    - PAID: 이미 모든 처리가 끝난 경우 → 결과만 저장하고 반환
            OrderPaymentInfo paymentInfo = orderService.getPaymentInfo(userId, orderId);

            if (paymentInfo.orderStatus() == OrderStatus.PAYMENT_APPROVED
                    || paymentInfo.orderStatus() == OrderStatus.PAID) {
                OrderResponseDto response = orderService.finalizeApprovedPayment(userId, orderId);
                paymentIdempotencyService.saveResponse(userId, orderId, idempotencyKey, response);
                return response;
            }

            // 6. 주문 상태를 PAYMENT_PENDING으로 전이 (비관적 락으로 동시 결제 시도 차단)
            paymentInfo = orderService.startPayment(userId, orderId);

            // 7. 외부 PG(결제 대행사) 결제 승인 요청
            paymentService.pay(orderId, paymentInfo.totalPrice(), request);

            // 8. PG 승인 성공을 DB에 기록 (PAYMENT_APPROVED 상태로 전이)
            //    → 이후 내부 확정이 실패해도 이 상태를 기준으로 재시도 가능
            orderService.markPaymentApproved(userId, orderId);

            // 9. DB 재고 차감 및 주문 상태를 PAID로 확정
            OrderResponseDto response = retryFinalizeApprovedPayment(userId, orderId);

            // 10. Redis 예약키 삭제 및 멱등성 응답 저장
            paymentIdempotencyService.saveResponse(userId, orderId, idempotencyKey, response);

            // 11. 장바구니 비우기 (실패해도 결제 결과에 영향 없음)
            try {
                cartService.clearCart(userId);
            } catch (Exception e) {
                log.warn("[장바구니 비우기 실패] orderId={}, userId={}", orderId, userId, e);
            }

            meterRegistry.counter("payment.result", "status", "success").increment();
            return response;

        } catch (PaymentFailedException e) {
            // PG 결제 승인 실패 → 주문 상태를 PAYMENT_FAILED로 전이 (예약은 유지, 재결제 가능)
            orderService.failPayment(userId, orderId, e.getMessage());
            meterRegistry.counter("payment.result", "status", "failed").increment();
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } finally {
            // 성공/실패 여부와 관계없이 분산 락 해제
            paymentIdempotencyService.releaseLock(userId, orderId, idempotencyKey);
        }
    }

    public OrderResponseDto cancelOrder(Long userId, Long orderId) {
        OrderPaymentInfo cancelInfo = orderService.requestCancel(userId, orderId);

        try {
            paymentService.cancel(orderId, cancelInfo.totalPrice());
            return orderService.completeRefund(userId, orderId);
        } catch (PaymentFailedException e) {
            orderService.failRefund(userId, orderId, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    public OrderResponseDto retryRefund(Long userId, Long orderId) {
        OrderPaymentInfo paymentInfo = orderService.getPaymentInfo(userId, orderId);

        if (paymentInfo.orderStatus() == OrderStatus.REFUNDED) {
            return orderService.completeRefund(userId, orderId);
        }

        if (paymentInfo.orderStatus() != OrderStatus.CANCEL_FAILED) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        try {
            paymentService.cancel(orderId, paymentInfo.totalPrice());
            return orderService.completeRefund(userId, orderId);
        } catch (PaymentFailedException e) {
            orderService.failRefund(userId, orderId, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    /**
     * 기본 주문 요청 검증
     */
    private void validateOrderRequest(OrderRequestDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (request.getCheckoutToken() == null || request.getCheckoutToken().isBlank()) {
            throw new BusinessException(ErrorCode.HAS_NO_CHECKOUT_TOKEN);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 사용자별·상품별 주문 요청 제한
     */
    private void validateRateLimit(Long userId, OrderRequestDto request) {
        for (OrderItemsListDto item : request.getItems()) {
            boolean allowed = orderRateLimitService.allow(userId, item.getProductId());

            if (!allowed) {
                meterRegistry.counter("order.rejected", "reason", "rate_limit").increment();
                throw new BusinessException(ErrorCode.TOO_MANY_ORDER_REQUESTS);
            }
        }
    }

    /**
     * Redis 품절 캐시를 이용한 빠른 거절
     */
    private void validateSoldOutCache(OrderRequestDto request) {
        for (OrderItemsListDto item : request.getItems()) {
            boolean soldOut = soldOutCacheService.isSoldOut(item.getProductId());

            if (soldOut) {
                meterRegistry.counter("order.rejected", "reason", "sold_out_cache").increment();
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }
}



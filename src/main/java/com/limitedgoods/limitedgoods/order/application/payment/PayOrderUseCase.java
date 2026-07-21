package com.limitedgoods.limitedgoods.order.application.payment;

import com.limitedgoods.limitedgoods.cart.service.CartService;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.payment.OrderPaymentInfo;
import com.limitedgoods.limitedgoods.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.payment.service.PaymentFailedException;
import com.limitedgoods.limitedgoods.order.application.payment.idempotency.OrderPaymentIdempotencyService;
import com.limitedgoods.limitedgoods.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderUseCase {

    private final OrderPaymentService orderPaymentService;
    private final ApprovedPaymentFinalizer paymentFinalizer;

    private final PaymentService paymentService;
    private final OrderPaymentIdempotencyService orderPaymentIdempotencyService;

    private final CartService cartService;

    public OrderResponseDto execute(
            Long userId,
            Long orderId,
            PaymentRequestDto request,
            String idempotencyKey
    ) {
        OrderResponseDto savedResponse =
                orderPaymentIdempotencyService.getSavedResponse(
                        userId,
                        orderId,
                        idempotencyKey
                );

        if (savedResponse != null) {
            return savedResponse;
        }

        boolean locked =
                orderPaymentIdempotencyService.acquireLock(
                        userId,
                        orderId,
                        idempotencyKey
                );

        if (!locked) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        try {
            return processPayment(
                    userId,
                    orderId,
                    request,
                    idempotencyKey
            );
        } finally {
            releaseLockBestEffort(
                    userId,
                    orderId,
                    idempotencyKey
            );
        }
    }

    private OrderResponseDto processPayment(
            Long userId,
            Long orderId,
            PaymentRequestDto request,
            String idempotencyKey
    ) {
        OrderPaymentInfo paymentInfo = orderPaymentService.getPaymentInfo(userId, orderId);

        /*
         * PG 승인은 이미 성공했다.
         * 외부 PG를 다시 호출하지 않고 내부 확정만 수행한다.
         */
        if (paymentInfo.orderStatus() == OrderStatus.PAYMENT_APPROVED) {

            OrderResponseDto response = paymentFinalizer.finalizePayment(userId, orderId);

            return completeSuccess(
                    userId,
                    orderId,
                    idempotencyKey,
                    response
            );
        }

        /*
         * 이미 모든 결제 처리가 완료됐다.
         */
        if (paymentInfo.orderStatus() == OrderStatus.PAID) {
            OrderResponseDto response = paymentFinalizer.finalizePayment(userId, orderId);

            return completeSuccess(
                    userId,
                    orderId,
                    idempotencyKey,
                    response
            );
        }

        paymentInfo = orderPaymentService.startPayment(userId, orderId);

        requestPgPayment(
                userId,
                orderId,
                paymentInfo.totalPrice(),
                request
        );

        orderPaymentService.markPaymentApproved(userId, orderId);

        OrderResponseDto response = paymentFinalizer.finalizePayment(userId, orderId);

        return completeSuccess(
                userId,
                orderId,
                idempotencyKey,
                response
        );
    }

    private void requestPgPayment(
            Long userId,
            Long orderId,
            Long totalPrice,
            PaymentRequestDto request
    ) {
        try {
            paymentService.pay(
                    orderId,
                    totalPrice,
                    request
            );
        } catch (PaymentFailedException exception) {
            orderPaymentService.failPayment(
                    userId,
                    orderId,
                    exception.getMessage()
            );

            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    private OrderResponseDto completeSuccess(
            Long userId,
            Long orderId,
            String idempotencyKey,
            OrderResponseDto response
    ) {
        saveResponseBestEffort(
                userId,
                orderId,
                idempotencyKey,
                response
        );

        clearCartBestEffort(userId, orderId);

        return response;
    }

    private void saveResponseBestEffort(
            Long userId,
            Long orderId,
            String idempotencyKey,
            OrderResponseDto response
    ) {
        try {
            orderPaymentIdempotencyService.saveResponse(
                    userId,
                    orderId,
                    idempotencyKey,
                    response
            );
        } catch (Exception exception) {
            /*
             * DB 주문 상태는 이미 PAID다.
             * Redis 응답 저장 실패가 결제 성공을 실패 응답으로
             * 바꾸지 않도록 예외를 격리한다.
             */
            log.error(
                    "[결제 멱등 응답 저장 실패] userId={}, orderId={}",
                    userId,
                    orderId,
                    exception
            );
        }
    }

    private void clearCartBestEffort(
            Long userId,
            Long orderId
    ) {
        try {
            cartService.clearCart(userId);
        } catch (Exception exception) {
            log.warn(
                    "[결제 후 장바구니 정리 실패] userId={}, orderId={}",
                    userId,
                    orderId,
                    exception
            );
        }
    }

    private void releaseLockBestEffort(
            Long userId,
            Long orderId,
            String idempotencyKey
    ) {
        try {
            orderPaymentIdempotencyService.releaseLock(
                    userId,
                    orderId,
                    idempotencyKey
            );
        } catch (Exception exception) {
            /*
             * 락 해제 실패가 이미 성공한 결제 응답을 덮어쓰지 않도록 한다.
             * 락에는 TTL이 있어야 한다.
             */
            log.error(
                    "[결제 멱등 락 해제 실패] userId={}, orderId={}",
                    userId,
                    orderId,
                    exception
            );
        }
    }
}
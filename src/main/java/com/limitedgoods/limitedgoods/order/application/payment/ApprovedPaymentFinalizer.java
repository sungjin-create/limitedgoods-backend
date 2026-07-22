package com.limitedgoods.limitedgoods.order.application.payment;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovedPaymentFinalizer {

    private final OrderPaymentService orderPaymentService;

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1_000)
    )
    public OrderResponseDto finalizePayment(
            Long userId,
            Long orderId
    ) {
        return orderPaymentService.finalizeApprovedPayment(userId, orderId);
    }

    @Recover
    public OrderResponseDto recover(
            TransientDataAccessException exception,
            Long userId,
            Long orderId
    ) {
        log.error(
                "[결제 내부 확정 최종 실패] userId={}, orderId={}",
                userId,
                orderId,
                exception
        );

        throw new BusinessException(ErrorCode.PAYMENT_FINALIZATION_FAILED);
    }
}

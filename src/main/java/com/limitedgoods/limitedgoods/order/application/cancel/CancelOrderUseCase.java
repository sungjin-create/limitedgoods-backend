package com.limitedgoods.limitedgoods.order.application.cancel;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.application.cancel.dto.RefundCommand;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponse;
import com.limitedgoods.limitedgoods.payment.exception.PaymentNetworkException;
import com.limitedgoods.limitedgoods.payment.exception.PaymentRefundDeclinedException;
import com.limitedgoods.limitedgoods.payment.exception.PaymentTimeoutException;
import com.limitedgoods.limitedgoods.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderCancellationService cancellationService;
    private final PaymentService paymentService;

    public OrderResponse execute(
            Long userId,
            Long orderId
    ) {
        RefundCommand command = cancellationService.prepareRefund(userId, orderId);

        try {
            paymentService.cancel(
                    command.pgTransactionId(),
                    command.amount(),
                    command.idempotencyKey()
            );

            return cancellationService.completeRefund(userId, orderId);

        } catch (PaymentRefundDeclinedException exception) {
            cancellationService.failRefund(
                    userId,
                    orderId,
                    exception.getMessage()
            );

            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);

        } catch (PaymentTimeoutException | PaymentNetworkException exception) {
            // 실제로 PG 환불에 성공했을 수도 있으므로
            // CANCEL_FAILED로 변경하지 않는다.
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_RESULT_UNKNOWN);
        }
    }
}
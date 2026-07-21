package com.limitedgoods.limitedgoods.order.application.create;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.order.application.create.idempotency.OrderRequestFingerprintGenerator;
import com.limitedgoods.limitedgoods.order.dto.request.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.policy.OrderProductValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private static final long ORDER_EXPIRED_SECONDS = 300L;

    private final OrderCreatePreconditionChecker preconditionChecker;
    private final OrderAdmissionCoordinator admissionCoordinator;
    private final OrderRequestFingerprintGenerator fingerprintGenerator;
    private final OrderCreateTransactionService orderCreateTransactionService;


    public OrderResponseDto execute(
            Long userId,
            OrderRequestDto request
    ) {
        preconditionChecker.validateRequest(request);

        String checkoutToken = request.checkoutToken();
        String requestFingerprint = fingerprintGenerator.generate(request.items());

        OrderResponseDto existing =
                orderCreateTransactionService.findIdempotentOrder(
                    userId,
                    checkoutToken,
                    requestFingerprint
        );

        if (existing != null) {
            return existing;
        }

        OrderProductValidationResult validationResult =
                preconditionChecker.checkNewOrder(
                        userId,
                        request
                );

        Optional<OrderAdmissionClaim> admissionClaim =
                admissionCoordinator.claimIfRequired(
                        request.admissionToken(),
                        userId,
                        validationResult.admissionProductId(),
                        checkoutToken,
                        requestFingerprint
                );

        try {
            OrderResponseDto order =
                    orderCreateTransactionService.createOrder(
                            userId,
                            request.items(),
                            ORDER_EXPIRED_SECONDS,
                            checkoutToken,
                            requestFingerprint
                    );

            admissionCoordinator.completeAfterOrderCreated(admissionClaim);

            return order;

        } catch (BusinessException exception) {
            admissionCoordinator.releaseAfterBusinessFailure(
                    admissionClaim
            );

            throw exception;

        } catch (RuntimeException exception) {
            admissionCoordinator.retainAfterUnknownFailure(
                    admissionClaim,
                    exception
            );

            throw exception;
        }
    }
}

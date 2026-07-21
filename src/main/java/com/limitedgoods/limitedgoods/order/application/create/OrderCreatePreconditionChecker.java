package com.limitedgoods.limitedgoods.order.application.create;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.request.OrderItemsListDto;
import com.limitedgoods.limitedgoods.order.policy.OrderProductValidationResult;
import com.limitedgoods.limitedgoods.order.dto.request.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.policy.ProductOrderPolicy;
import com.limitedgoods.limitedgoods.order.infrastructure.ratelimit.OrderRateLimiter;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OrderCreatePreconditionChecker {

    private final OrderRateLimiter orderRateLimiter;
    private final ProductSoldOutCacheService productSoldOutCacheService;
    private final ProductOrderPolicy productOrderPolicy;

    public void validateRequest(OrderRequestDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (request.getCheckoutToken() == null || request.getCheckoutToken().isBlank()) {
            throw new BusinessException( ErrorCode.HAS_NO_CHECKOUT_TOKEN);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Set<Long> productIds = new HashSet<>();

        for (OrderItemsListDto item : request.getItems()) {
            if (item == null
                    || item.getProductId() == null
                    || item.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }

            if (!productIds.add(item.getProductId())) {
                throw new BusinessException(
                        ErrorCode.DUPLICATE_ORDER_PRODUCT
                );
            }
        }
    }

    /**
     * 신규 주문에만 필요한 외부 상태 및 상품 정책 검증
     */
    public OrderProductValidationResult checkNewOrder(
            Long userId,
            OrderRequestDto request
    ) {
        validateRateLimit(userId, request);
        validateSoldOutCache(request);

        return productOrderPolicy.validate(
                request.getItems()
        );
    }

    private void validateRateLimit(
            Long userId,
            OrderRequestDto request
    ) {
        for (OrderItemsListDto item : request.getItems()) {
            boolean allowed = orderRateLimiter.allow(
                    userId,
                    item.getProductId()
            );

            if (!allowed) {
                throw new BusinessException(
                        ErrorCode.TOO_MANY_ORDER_REQUESTS
                );
            }
        }
    }

    private void validateSoldOutCache(
            OrderRequestDto request
    ) {
        for (OrderItemsListDto item : request.getItems()) {
            boolean soldOut = productSoldOutCacheService.isSoldOut(
                    item.getProductId()
            );

            if (soldOut) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK
                );
            }
        }
    }
}

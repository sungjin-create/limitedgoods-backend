package com.limitedgoods.limitedgoods.order.application.support;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderAccessService {

    private final OrderRepository orderRepository;

    /**
     * 사용자 소유 주문을 잠금 없이 조회한다.
     */
    @Transactional(readOnly = true)
    public Order getOwnedOrder(
            Long orderId,
            Long userId
    ) {
        return orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND
                ));
    }

    /**
     * 사용자 소유 주문을 비관적 쓰기 잠금으로 조회한다.
     *
     * 상태 변경 서비스의 기존 트랜잭션 안에서 호출해야 한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Order getOwnedOrderForUpdate(
            Long orderId,
            Long userId
    ) {
        return orderRepository
                .findByIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND
                ));
    }

    /**
     * 스케줄러 등 시스템 작업에서 사용자 조건 없이 주문을 잠근다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Order getOrderForUpdate(Long orderId) {
        return orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ORDER_NOT_FOUND
                ));
    }
}

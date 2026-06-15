package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.AdminOrderSearchCondition;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<AdminOrderResponseDto> searchOrders(
            AdminOrderSearchCondition condition,
            Pageable pageable
    ) {
        LocalDateTime fromDate = condition.getFrom() != null
                ? condition.getFrom().atStartOfDay()
                : null;

        LocalDateTime toDate = condition.getTo() != null
                ? condition.getTo().plusDays(1).atStartOfDay()
                : null;

        return orderRepository.searchAdminOrders(
                condition.getStatus(),
                condition.getUserId(),
                fromDate,
                toDate,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public AdminOrderResponseDto getOrderDetail(Long orderId) {
        return orderRepository.findAdminOrderDetail(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}

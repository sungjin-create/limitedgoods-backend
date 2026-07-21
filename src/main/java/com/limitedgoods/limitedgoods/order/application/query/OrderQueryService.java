package com.limitedgoods.limitedgoods.order.application.query;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.application.mapper.OrderResponseMapper;
import com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.dto.response.OrderSummaryResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderResponseMapper orderResponseMapper;

    @Transactional(readOnly = true)
    public List<OrderSummaryResponseDto> findMyOrders(Long userId) {
        return orderRepository.findMyOrderSummaries(userId);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto findOrderDetail(
            Long userId,
            Long orderId
    ) {
        Order order = findOrder(userId, orderId);

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithProduct(orderId);

        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return orderResponseMapper.toDetailResponse(order, orderItems);
    }

    private Order findOrder(
            Long userId,
            Long orderId
    ) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

}

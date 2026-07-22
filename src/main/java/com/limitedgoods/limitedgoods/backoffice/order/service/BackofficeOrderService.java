package com.limitedgoods.limitedgoods.backoffice.order.service;

import com.limitedgoods.limitedgoods.backoffice.order.dto.*;
import com.limitedgoods.limitedgoods.backoffice.order.query.BackofficeOrderQueryRepository;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.application.history.OrderStatusHistoryService;
import com.limitedgoods.limitedgoods.order.application.support.OrderAccessService;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.user.application.support.UserAccessService;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackofficeOrderService {

    private final BackofficeOrderQueryRepository orderQueryRepository;
    private final OrderAccessService orderAccessService;
    private final UserAccessService userAccessService;
    private final UserRepository userRepository;
    private final OrderStatusHistoryService historyService;

    @Transactional(readOnly = true)
    public OrdersResponse findBackofficeOrders(LocalDateTime startAt, LocalDateTime endAt){
        OrderSummaryResponse summary;
        if (startAt == null || endAt == null) {
            summary =  orderQueryRepository.getBackofficeOrderSummary();
        }  else {
            summary = orderQueryRepository.getBackofficeOrderSummary(startAt, endAt);
        }

        List<OrderFlatResponse> ordersFlatResponseList;
        if (startAt == null || endAt == null) {
            ordersFlatResponseList =  orderQueryRepository.findBackofficeOrdersFlat();
        }  else {
            ordersFlatResponseList = orderQueryRepository.findBackofficeOrdersFlat(startAt, endAt);
        }

        Map<Long, OrderResponse> orderResponseMap = new LinkedHashMap<>();
        for(OrderFlatResponse flat: ordersFlatResponseList) {
            OrderResponse orderResponse =
                    orderResponseMap.computeIfAbsent(flat.orderId(), id ->
                            new OrderResponse(
                                    flat.orderId(),
                                    flat.email(),
                                    flat.totalPrice(),
                                    flat.status(),
                                    new ArrayList<>(),
                                    flat.createdAt())
                    );

            orderResponse.orderItems().add(
                    new OrderItemResponse(
                            flat.productId(),
                            flat.productName(),
                            flat.quantity(),
                            flat.price()
                    ));
        }

        return OrdersResponse.builder()
                .summary(summary)
                .orders(new ArrayList<>(orderResponseMap.values()))
                .build();
    }

    @Transactional
    public OrderStatusResponse completeOrder(Long userId, Long orderId){
        User changedByUser = userAccessService.getUser(userId);
        Order order = orderAccessService.getOrderForUpdate(orderId);

        // 같은 요청이 재전송된 경우 중복 이력을 만들지 않고 성공으로 처리
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return OrderStatusResponse.from(order);
        }

        OrderStatus previousStatus = order.getStatus();

        order.markComplete();

        historyService.record(
                order,
                previousStatus,
                OrderStatus.COMPLETED,
                "주문 완료 처리",
                changedByUser
        );

        return OrderStatusResponse.from(order);
    }
}

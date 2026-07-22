package com.limitedgoods.limitedgoods.backoffice.order.service;

import com.limitedgoods.limitedgoods.backoffice.order.dto.*;
import com.limitedgoods.limitedgoods.backoffice.order.query.BackofficeOrderQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackofficeOrderService {

    private final BackofficeOrderQueryRepository orderQueryRepository;

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
}

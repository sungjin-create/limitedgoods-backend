package com.limitedgoods.limitedgoods.backoffice.order.service;

import com.limitedgoods.limitedgoods.backoffice.order.dto.*;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
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

    private final OrderRepository orderRepository;

    public BackofficeMonitoringResponse getBackofficeMonitoring(LocalDateTime startAt, LocalDateTime endAt){
        BackofficeMonitoringSummaryResponse getSummary;
        if (startAt == null || endAt == null) {
            getSummary =  orderRepository.getBackofficeMonitoringSummary();
        }  else {
            getSummary = orderRepository.getBackofficeMonitoringSummary(startAt, endAt);
        }

        BackofficeMonitoringSummaryResponse summary = BackofficeMonitoringSummaryResponse.builder()
                .paymentPendingCount(getSummary.getPaymentPendingCount())
                .paidCount(getSummary.getPaidCount())
                .cancelRequestedCount(getSummary.getCancelRequestedCount())
                .paymentFailedCount(getSummary.getPaymentFailedCount())
                .build();



        List<BackofficeMonitoringOrderFlatResponse> ordersFlatResponseList;
        if (startAt == null || endAt == null) {
            ordersFlatResponseList =  orderRepository.findBackofficeMonitoringOrderFlat();
        }  else {
            ordersFlatResponseList = orderRepository.findBackofficeMonitoringOrderFlat(startAt, endAt);
        }

        Map<Long, BackofficeMonitoringOrdersResponse> monitoringOrderMap = new LinkedHashMap<>();
        for(BackofficeMonitoringOrderFlatResponse flat: ordersFlatResponseList) {
            BackofficeMonitoringOrdersResponse monitoringOrdersResponse =
                    monitoringOrderMap.computeIfAbsent(flat.getOrderId(), id -> BackofficeMonitoringOrdersResponse.builder()
                            .orderId(flat.getOrderId())
                            .userEmail(flat.getEmail())
                            .totalPrice(flat.getTotalPrice())
                            .status(flat.getStatus().toString())
                            .createdAt(flat.getCreatedAt())
                            .orderItems(new ArrayList<>())
                            .build());

            monitoringOrdersResponse.getOrderItems().add(BackofficeMonitoringOrderItemResponse.builder()
                            .productId(flat.getProductId())
                            .productName(flat.getProductName())
                            .quantity(flat.getQuantity())
                            .price(flat.getPrice())
                            .build());
        }

        return BackofficeMonitoringResponse.builder()
                .summary(summary)
                .orders(new ArrayList<>(monitoringOrderMap.values()))
                .build();
    }
}

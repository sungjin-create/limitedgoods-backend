package com.limitedgoods.limitedgoods.backoffice.monitoring.service;

import com.limitedgoods.limitedgoods.backoffice.client.PrometheusClient;
import com.limitedgoods.limitedgoods.backoffice.monitoring.dto.BackofficeBusinessMetricResponse;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackofficeMonitoringService {

    private final PrometheusClient prometheusClient;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Map<String, Object> getOverview() {
        Map requestsPerSecond = prometheusClient.query(
                "sum(rate(http_server_requests_seconds_count[1m]))"
        );

        Map errorRate = prometheusClient.query(
                "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m]))"
        );

        Map avgLatency = prometheusClient.query(
                "sum(rate(http_server_requests_seconds_sum[1m])) / sum(rate(http_server_requests_seconds_count[1m]))"
        );

        return Map.of(
                "requestsPerSecond", requestsPerSecond,
                "errorRate", errorRate,
                "avgLatency", avgLatency
        );
    }

    public BackofficeBusinessMetricResponse getBusinessMetrics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        List<OrderStatus> revenueStatuses = List.of(
                OrderStatus.PAID,
                OrderStatus.COMPLETED
        );

        long todayOrderCount = orderRepository.countOrdersCreatedBetween(
                startOfToday,
                startOfTomorrow
        );

        long todayPaidOrderCount = orderRepository.countPaidOrdersBetween(
                startOfToday,
                startOfTomorrow,
                revenueStatuses
        );

        long todayRevenue = orderRepository.sumRevenueBetween(
                startOfToday,
                startOfTomorrow,
                revenueStatuses
        );

        long soldOutProductCount = productRepository.countSoldOutProducts();

        return BackofficeBusinessMetricResponse.builder()
                .todayOrderCount(todayOrderCount)
                .todayPaidOrderCount(todayPaidOrderCount)
                .todayRevenue(todayRevenue)
                .soldOutProductCount(soldOutProductCount)
                .build();
    }
}
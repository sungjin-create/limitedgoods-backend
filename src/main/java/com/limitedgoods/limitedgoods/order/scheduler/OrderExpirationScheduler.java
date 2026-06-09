package com.limitedgoods.limitedgoods.order.scheduler;

import com.limitedgoods.limitedgoods.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60000)
    public void expireOrders() {
        List<Long> expiredOrderIds = orderService.findExpiredOrderIds();

        for (Long orderId : expiredOrderIds) {
            try {
                orderService.expireOrder(orderId);
            } catch (Exception e) {
                log.warn("주문 만료 처리 실패. orderId={}", orderId, e);
            }
        }
    }
}

package com.limitedgoods.limitedgoods.event.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "notification-consumer")
    public void consume(String payload) {
        log.info("[ORDER EVENT RECEIVED] payload={}", payload);

        // TODO:
        // 1. 알림 발송
        // 2. 주문 이벤트 이력 저장
        // 3. 통계 데이터 업데이트
    }
}

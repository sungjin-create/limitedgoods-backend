package com.limitedgoods.limitedgoods.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.event.processed.dto.KafkaEventEnvelope;
import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import com.limitedgoods.limitedgoods.event.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final String CONSUMER_GROUP = "notification-consumer";

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "order-events",
            groupId = CONSUMER_GROUP
    )
    @Transactional
    public void consume(String message) throws JsonProcessingException {
        KafkaEventEnvelope envelope =
                objectMapper.readValue(message, KafkaEventEnvelope.class);

        if (processedEventRepository.existsByEventIdAndConsumerGroup(
                envelope.eventId(),
                CONSUMER_GROUP
        )) {
            log.info("이미 처리된 Kafka 이벤트입니다. eventId={}", envelope.eventId());
            return;
        }

        log.info("Kafka 이벤트 처리 시작. eventId={}, eventType={}",
                envelope.eventId(),
                envelope.eventType());

        // 여기서 실제 후속 처리
        // 예: 알림 저장, 로그 저장, 이메일 발송 요청 등
        processBusinessLogic(envelope);

        processedEventRepository.save(
                ProcessedEvent.create(envelope.eventId(), CONSUMER_GROUP)
        );

        log.info("Kafka 이벤트 처리 완료. eventId={}", envelope.eventId());
    }

    private void processBusinessLogic(KafkaEventEnvelope envelope) {
        log.info("주문 이벤트 처리 payload={}", envelope.payload());
    }
}

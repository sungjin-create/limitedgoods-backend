package com.limitedgoods.limitedgoods.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.event.processed.dto.KafkaEventEnvelope;
import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import com.limitedgoods.limitedgoods.event.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    public void consume(ConsumerRecord<String, String> record){
        KafkaEventEnvelope envelope = null;

        try {
            envelope = objectMapper.readValue(
                    record.value(),
                    KafkaEventEnvelope.class
            );

            if (processedEventRepository.existsByEventIdAndConsumerGroup(envelope.eventId(), CONSUMER_GROUP )) {
                return;
            }

            processBusinessLogic(envelope);

            processedEventRepository.save(
                    ProcessedEvent.create(
                            envelope.eventId(),
                            CONSUMER_GROUP
                    )
            );

        } catch (JsonProcessingException e) {
            log.error(
                    "event=kafka_consume_failed component=kafka-consumer " +
                            "eventId={} consumerGroup={} topic={} partition={} offset={}",
                    envelope != null ? envelope.eventId() : null,
                    CONSUMER_GROUP,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e
            );
            throw new IllegalStateException("Kafka 메시지 역직렬화 실패");
        } catch (Exception e) {
            log.error(
                    "event=kafka_consume_failed component=kafka-consumer " +
                            "eventId={} consumerGroup={} topic={} partition={} offset={}",
                    envelope != null ? envelope.eventId() : null,
                    CONSUMER_GROUP,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e
            );

            throw e;
        }
    }

    private void processBusinessLogic(KafkaEventEnvelope envelope) {
        log.info("주문 이벤트 처리 payload={}", envelope.payload());
    }
}

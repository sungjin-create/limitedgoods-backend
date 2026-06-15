package com.limitedgoods.limitedgoods.event.outbox.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import com.limitedgoods.limitedgoods.event.processed.dto.KafkaEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final String TOPIC = "order-events";
    private static final int RETRY_LIMIT = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publish.delay}")
    public void publish() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                        List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
                        RETRY_LIMIT
                );

        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            KafkaEventEnvelope envelope = new KafkaEventEnvelope(
                    event.getId(),
                    event.getEventType().name(),
                    event.getPayload()
            );

            String message = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId().toString(),
                    message
            ).whenComplete((result, ex) -> {
                if (ex == null) {
                    outboxEventService.markPublished(event.getId());
                } else {
                    outboxEventService.markFailed(event.getId(), ex);
                }
            });

        } catch (JsonProcessingException e) {
            outboxEventService.markFailed(event.getId(), e);
        }
    }

}

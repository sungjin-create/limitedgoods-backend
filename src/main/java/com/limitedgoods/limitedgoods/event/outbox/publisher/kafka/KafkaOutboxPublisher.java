//package com.limitedgoods.limitedgoods.event.outbox.publisher.kafka;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
//import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
//import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
//import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventWriter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//@Profile("!internal-worker")
//@RequiredArgsConstructor
//public class KafkaOutboxPublisher {
//
//    private static final String TOPIC = "order-events";
//    private static final int RETRY_LIMIT = 5;
//
//    private final OutboxEventRepository outboxEventRepository;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final OutboxEventWriter outboxEventWriter;
//    private final ObjectMapper objectMapper;
//
//    @Scheduled(fixedDelayString = "${outbox.publish.delay}")
//    public void publish() {
//        List<OutboxEvent> events =
//                outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
//                        List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
//                        RETRY_LIMIT
//                );
//
//        for (OutboxEvent event : events) {
//            publishOne(event);
//        }
//    }
//
//    private void publishOne(OutboxEvent event) {
//        try {
//            KafkaEventEnvelope envelope = new KafkaEventEnvelope(
//                    event.getId(),
//                    event.getEventType().name(),
//                    event.getPayload()
//            );
//
//            String message = objectMapper.writeValueAsString(envelope);
//
//            kafkaTemplate.send(
//                    TOPIC,
//                    event.getAggregateId().toString(),
//                    message
//            ).whenComplete((result, ex) -> {
//                if (ex == null) {
//                    outboxEventWriter.markPublished(event.getId());
//                } else {
//                    outboxEventWriter.markFailed(event.getId(), ex);
//                    log.error(
//                            "event=kafka_publish_failed component=kafka-producer " +
//                                    "eventId={} eventType={} aggregateId={} topic={}",
//                            event.getId(),
//                            event.getEventType(),
//                            event.getAggregateId(),
//                            TOPIC,
//                            ex
//                    );
//                }
//            });
//
//        } catch (JsonProcessingException e) {
//            outboxEventWriter.markFailed(event.getId(), e);
//            log.error(
//                    "event=kafka_serialization_failed component=kafka-producer " +
//                            "eventId={} eventType={} aggregateId={}",
//                    event.getId(),
//                    event.getEventType(),
//                    event.getAggregateId(),
//                    e
//            );
//        }
//    }
//
//}

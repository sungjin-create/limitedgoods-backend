package com.limitedgoods.limitedgoods.event.outbox.publisher;

import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.publish.delay}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            kafkaTemplate.send(
                    "order-events",
                    event.getAggregateId().toString(),
                    event.getPayload()
            );

            event.markPublished();
        }
    }
}

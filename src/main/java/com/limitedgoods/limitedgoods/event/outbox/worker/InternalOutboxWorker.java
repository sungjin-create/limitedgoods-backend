package com.limitedgoods.limitedgoods.event.outbox.worker;

import com.limitedgoods.limitedgoods.event.outbox.service.InternalOutboxProcessor;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("internal-worker")
@RequiredArgsConstructor
public class InternalOutboxWorker {
    private static final int RETRY_LIMIT = 5;
    private final OutboxEventRepository outboxEventRepository;
    private final InternalOutboxProcessor internalOutboxProcessor;
    private final OutboxEventService outboxEventService;

    @Scheduled(fixedDelayString = "${outbox.publish.delay}")
    public void processPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED), RETRY_LIMIT);
        for (OutboxEvent event : events) {
            try {
                internalOutboxProcessor.process(event.getId());
            } catch (Exception exception) {
                outboxEventService.markFailed(event.getId(), exception);
                log.error("event=internal_outbox_process_failed eventId={} eventType={}", event.getId(), event.getEventType(), exception);
            }
        }
    }
}

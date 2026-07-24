package com.limitedgoods.limitedgoods.event.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void append(
            OutboxEventType eventType,
            String aggregateType,
            Long aggregateId,
            Object payload
    ) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.create(
                    eventType,
                    aggregateType,
                    aggregateId,
                    json
            );

            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox event serialization failed", e);
        }
    }

    @Transactional
    public void markPublished(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow();

        event.markPublished();
    }

    @Transactional
    public void markFailed(Long eventId, Throwable ex) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow();

        event.markFailed(ex.getMessage());
    }
}

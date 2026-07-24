package com.limitedgoods.limitedgoods.event.outbox.publisher.kafka;

public record KafkaEventEnvelope(
        Long eventId,
        String eventType,
        String payload
) {
}
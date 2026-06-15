package com.limitedgoods.limitedgoods.event.processed.dto;

public record KafkaEventEnvelope(
        Long eventId,
        String eventType,
        String payload
) {
}
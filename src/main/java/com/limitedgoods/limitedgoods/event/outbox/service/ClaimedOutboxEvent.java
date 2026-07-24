package com.limitedgoods.limitedgoods.event.outbox.service;

import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;

import java.util.UUID;

public record ClaimedOutboxEvent(
        Long eventId,
        UUID claimToken,
        OutboxEventType eventType
) {
}
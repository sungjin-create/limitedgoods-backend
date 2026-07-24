package com.limitedgoods.limitedgoods.event.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DEAD
}

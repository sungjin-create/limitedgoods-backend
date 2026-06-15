package com.limitedgoods.limitedgoods.event.outbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    private String aggregateType;
    private Long aggregateId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private LocalDateTime lastTriedAt;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static OutboxEvent create(
            OutboxEventType eventType,
            String aggregateType,
            Long aggregateId,
            String payload
    ) {
        OutboxEvent event = new OutboxEvent();
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.retryCount = 0;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount++;
        this.lastError = errorMessage;
        this.lastTriedAt = LocalDateTime.now();
    }

}

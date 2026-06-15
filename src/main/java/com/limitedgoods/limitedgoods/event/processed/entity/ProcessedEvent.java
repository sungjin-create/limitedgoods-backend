package com.limitedgoods.limitedgoods.event.processed.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_event",
                        columnNames = {"eventId", "consumerGroup"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    private String consumerGroup;

    private LocalDateTime processedAt;

    public static ProcessedEvent create(Long eventId, String consumerGroup) {
        ProcessedEvent event = new ProcessedEvent();
        event.eventId = eventId;
        event.consumerGroup = consumerGroup;
        event.processedAt = LocalDateTime.now();
        return event;
    }
}

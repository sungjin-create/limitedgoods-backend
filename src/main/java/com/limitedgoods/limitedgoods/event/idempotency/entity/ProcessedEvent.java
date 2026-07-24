package com.limitedgoods.limitedgoods.event.idempotency.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "internal_processed_event", uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "consumerName"}))
public class ProcessedEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long eventId;
    @Column(nullable = false) private String consumerName;

    public ProcessedEvent(Long eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }
}

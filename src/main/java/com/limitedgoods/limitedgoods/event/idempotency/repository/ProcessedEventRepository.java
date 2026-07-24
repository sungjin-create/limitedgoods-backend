package com.limitedgoods.limitedgoods.event.idempotency.repository;

import com.limitedgoods.limitedgoods.event.idempotency.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventIdAndConsumerName(Long eventId, String consumerName);
}

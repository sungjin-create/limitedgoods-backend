package com.limitedgoods.limitedgoods.event.processed.repository;

import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByEventIdAndConsumerName(Long eventId, String consumerName);
}

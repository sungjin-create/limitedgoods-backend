package com.limitedgoods.limitedgoods.event.outbox.repository;

import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            List<OutboxEventStatus> statuses,
            int retryLimit
    );
}

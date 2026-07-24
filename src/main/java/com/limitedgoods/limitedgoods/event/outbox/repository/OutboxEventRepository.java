package com.limitedgoods.limitedgoods.event.outbox.repository;

import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
            List<OutboxEventStatus> statuses,
            int retryLimit
    );

    @Query(
            value = """
                    SELECT *
                      FROM outbox_event
                     WHERE (
                            (
                                status IN ('PENDING', 'FAILED')
                                AND next_attempt_at <= :now
                            )
                            OR
                            (
                                status = 'PROCESSING'
                                AND processing_started_at <= :staleBefore
                            )
                           )
                       AND attempt_count < :maxAttempts
                     ORDER BY created_at ASC, id ASC
                     LIMIT :batchSize
                     FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> lockClaimableEvents(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("batchSize") int batchSize
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(
            value = """
                    UPDATE outbox_event
                       SET status = 'DEAD',
                           claim_token = NULL,
                           processing_started_at = NULL,
                           lease_expired_count =
                               lease_expired_count + 1,
                           last_error =
                               'Processing lease expired after max attempts'
                     WHERE status = 'PROCESSING'
                       AND processing_started_at <= :staleBefore
                       AND attempt_count >= :maxAttempts
                    """,
            nativeQuery = true
    )
    int markExhaustedProcessingAsDead(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxAttempts") int maxAttempts
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
              FROM OutboxEvent event
             WHERE event.id = :eventId
            """)
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("eventId")
            Long eventId
    );
}

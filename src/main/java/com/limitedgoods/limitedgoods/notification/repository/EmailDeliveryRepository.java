package com.limitedgoods.limitedgoods.notification.repository;

import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
    @Query(value = """
        SELECT *
          FROM internal_email_delivery
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
        """, nativeQuery = true)
    List<EmailDelivery> lockClaimableDeliveries(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("batchSize") int batchSize
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query(value = """
        UPDATE internal_email_delivery
           SET status = 'DEAD',
               claim_token = NULL,
               processing_started_at = NULL,
               lease_expired_count = lease_expired_count + 1,
               last_error = 'Processing lease expired after max attempts'
         WHERE status = 'PROCESSING'
           AND processing_started_at <= :staleBefore
           AND attempt_count >= :maxAttempts
        """, nativeQuery = true)
    int markExhaustedProcessingAsDead(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("maxAttempts") int maxAttempts
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT delivery
          FROM EmailDelivery delivery
         WHERE delivery.id = :deliveryId
        """)
    Optional<EmailDelivery> findByIdForUpdate(
            @Param("deliveryId") Long deliveryId
    );

    Page<EmailDelivery> findByStatus(
            EmailDelivery.Status status,
            Pageable pageable
    );
}

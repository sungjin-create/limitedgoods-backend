package com.limitedgoods.limitedgoods.payment.repository;

import com.limitedgoods.limitedgoods.payment.entity.PaymentAttempt;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select p
    from PaymentAttempt p
    where p.id = :attemptId
    """)
    Optional<PaymentAttempt> findByIdForUpdate(
            @Param("attemptId") Long attemptId
    );

    Optional<PaymentAttempt> findByOrderIdAndIdempotencyKey(
            Long orderId,
            String idempotencyKey
    );
}

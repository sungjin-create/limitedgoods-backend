package com.limitedgoods.limitedgoods.notification.service;

import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.notification.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailDeliveryStateService {
    private final EmailDeliveryRepository emailDeliveryRepository;

    @Transactional
    public List<ClaimedEmail> claimBatch(
            LocalDateTime now,
            LocalDateTime staleBefore,
            int maxAttempts,
            int batchSize
    ) {
        emailDeliveryRepository.markExhaustedProcessingAsDead(
                staleBefore,
                maxAttempts
        );

        List<EmailDelivery> deliveries =
                emailDeliveryRepository.lockClaimableDeliveries(
                        now,
                        staleBefore,
                        maxAttempts,
                        batchSize
                );

        return deliveries.stream()
                .map(delivery ->
                        new ClaimedEmail(
                                delivery.getId(),
                                delivery.markProcessing(now)
                        ))
                .toList();
    }

    @Transactional
    public void markSent(
            Long deliveryId,
            UUID claimToken,
            LocalDateTime now
    ) {
        EmailDelivery delivery = getForUpdate(deliveryId);
        delivery.markSent(claimToken, now);
    }

    @Transactional
    public void markRetryableFailure(
            ClaimedEmail claim,
            Throwable exception,
            int maxAttempts,
            LocalDateTime now
    ) {
        EmailDelivery delivery = getForUpdate(claim.deliveryId());

        delivery.markRetryableFailure(
                claim.claimToken(),
                exception.getMessage(),
                maxAttempts,
                now
        );
    }

    @Transactional
    public void markPermanentFailure(
            ClaimedEmail claim,
            Throwable exception
    ) {
        EmailDelivery delivery = getForUpdate(claim.deliveryId());

        delivery.markPermanentFailure(
                claim.claimToken(),
                exception.getMessage()
        );
    }

    private EmailDelivery getForUpdate(Long deliveryId) {
        return emailDeliveryRepository
                .findByIdForUpdate(deliveryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email delivery not found: " + deliveryId
                        )
                );
    }

}

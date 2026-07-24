package com.limitedgoods.limitedgoods.notification.worker;

import com.limitedgoods.limitedgoods.notification.service.ClaimedEmail;
import com.limitedgoods.limitedgoods.notification.service.EmailDeliveryService;
import com.limitedgoods.limitedgoods.notification.service.EmailDeliveryStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("internal-worker")
@ConditionalOnProperty(
        name = "app.mail.enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class EmailDeliveryWorker {

    private final EmailDeliveryStateService stateService;
    private final EmailDeliveryService deliveryService;

    @Value("${app.mail.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.mail.processing-lease-seconds:600}")
    private long processingLeaseSeconds;

    @Value("${app.mail.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.mail.retry-delay-ms:5000}")
    public void sendPendingEmails() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(processingLeaseSeconds);

        List<ClaimedEmail> claims = stateService.claimBatch(
                now,
                staleBefore,
                maxAttempts,
                batchSize
        );

        for (ClaimedEmail claim : claims) {
            try {
                deliveryService.send(claim);
            } catch (Exception exception) {
                log.error(
                        "event=email_delivery_unexpected_failure "
                                + "deliveryId={} errorType={}",
                        claim.deliveryId(),
                        exception.getClass().getSimpleName(),
                        exception
                );
            }
        }
    }
}

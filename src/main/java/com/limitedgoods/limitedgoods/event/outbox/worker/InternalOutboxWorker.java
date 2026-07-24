package com.limitedgoods.limitedgoods.event.outbox.worker;

import com.limitedgoods.limitedgoods.event.outbox.exception.OutboxClaimOwnershipLostException;
import com.limitedgoods.limitedgoods.event.outbox.service.ClaimedOutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.service.InternalOutboxProcessor;
import com.limitedgoods.limitedgoods.event.outbox.service.InternalOutboxStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("internal-worker")
@RequiredArgsConstructor
public class InternalOutboxWorker {

    private final InternalOutboxStateService stateService;
    private final InternalOutboxProcessor processor;

    @Value("${outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${outbox.batch-size:100}")
    private int batchSize;

    @Value("${outbox.processing-lease-seconds:300}")
    private long processingLeaseSeconds;

    @Scheduled(fixedDelayString = "${outbox.publish.delay:5000}")
    public void processPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusSeconds(processingLeaseSeconds);

        List<ClaimedOutboxEvent> claims =
                stateService.claimBatch(
                        now,
                        staleBefore,
                        maxAttempts,
                        batchSize
                );

        for (ClaimedOutboxEvent claim : claims) {
            processOne(claim);
        }
    }

    private void processOne(ClaimedOutboxEvent claim) {
        try {
            processor.process(claim);

        } catch (OutboxClaimOwnershipLostException exception) {
            /*
             * lease가 만료되어 다른 서버가 이미 다시 가져갔습니다.
             * 오래된 서버가 FAILED 상태로 덮어쓰면 안 됩니다.
             */
            log.warn(
                    "event=internal_outbox_claim_lost "
                            + "eventId={}",
                    claim.eventId()
            );

        } catch (Exception exception) {
            try {
                stateService.markFailed(
                        claim,
                        exception,
                        maxAttempts,
                        LocalDateTime.now()
                );

            } catch (OutboxClaimOwnershipLostException ownershipException) {
                exception.addSuppressed(ownershipException);

                log.warn(
                        "event=internal_outbox_failure_claim_lost "
                                + "eventId={}",
                        claim.eventId()
                );

                return;
            }

            log.error(
                    "event=internal_outbox_process_failed "
                            + "eventId={} eventType={}",
                    claim.eventId(),
                    claim.eventType(),
                    exception
            );
        }
    }
}
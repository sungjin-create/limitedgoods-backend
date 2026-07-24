package com.limitedgoods.limitedgoods.notification.service;

import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.notification.exception.EmailInfrastructureException;
import com.limitedgoods.limitedgoods.notification.exception.NonRetryableEmailException;
import com.limitedgoods.limitedgoods.notification.exception.RetryableEmailException;
import com.limitedgoods.limitedgoods.notification.infrastructure.mail.EmailProviderCircuit;
import com.limitedgoods.limitedgoods.notification.infrastructure.mail.EmailSender;
import com.limitedgoods.limitedgoods.notification.repository.EmailDeliveryRepository;
import com.limitedgoods.limitedgoods.notification.template.EmailContent;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateNotFoundException;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailDeliveryService {
    private final EmailDeliveryRepository emailDeliveryRepository;
    private final EmailSender emailSender;
    private final EmailDeliveryStateService stateService;
    private final EmailTemplateRegistry templateRegistry;
    private final EmailProviderCircuit providerCircuit;

    @Value("${app.mail.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.mail.infrastructure-backoff-ms:300000}")
    private long infrastructureBackoffMs;

    public void send(ClaimedEmail claim) {

        EmailDelivery delivery =
                emailDeliveryRepository
                        .findById(claim.deliveryId())
                        .orElseThrow();

        if (!delivery.isOwnedBy(claim.claimToken())) {
            return;
        }

        EmailContent content;

        try {
            content = templateRegistry.render(
                    delivery.getTemplateKey(),
                    delivery.getTemplateData()
            );
        } catch (EmailTemplateNotFoundException exception) {
            stateService.markPermanentFailure(claim, exception);
            return;
        }

        try {
            emailSender.send(
                    delivery.getRecipientEmail(),
                    content.subject(),
                    content.body()
            );

        } catch (EmailInfrastructureException exception) {
            Duration infrastructureBackoff = Duration.ofMillis(infrastructureBackoffMs);
            LocalDateTime nextAttemptAt = LocalDateTime.now().plus(infrastructureBackoff);

            providerCircuit.open(Instant.now(), Duration.ofMillis(infrastructureBackoffMs));

            log.error(
                    "event=email_provider_circuit_opened "
                            + "blockedUntil={} reason={}",
                    providerCircuit.getBlockedUntil(),
                    exception.getClass().getSimpleName()
            );

            try {
                stateService.releaseAfterInfrastructureFailure(
                        claim,
                        exception,
                        nextAttemptAt
                );
            } catch (Exception stateException) {
                exception.addSuppressed(stateException);

                log.error(
                        "event=email_infrastructure_failure_state_release_failed "
                                + "deliveryId={} errorType={}",
                        claim.deliveryId(),
                        stateException.getClass().getSimpleName(),
                        stateException
                );
            }

            // Worker가 나머지 배치를 중단할 수 있도록 다시 전달합니다.
            throw exception;

        } catch (NonRetryableEmailException exception) {
            stateService.markPermanentFailure(claim, exception);
            return;

        } catch (RetryableEmailException exception) {
            stateService.markRetryableFailure(
                    claim,
                    exception,
                    maxAttempts,
                    LocalDateTime.now()
            );
            return;
        }

        stateService.markSent(
                claim.deliveryId(),
                claim.claimToken(),
                LocalDateTime.now()
        );

    }

}

package com.limitedgoods.limitedgoods.notification.service;

import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.notification.exception.NonRetryableEmailException;
import com.limitedgoods.limitedgoods.notification.exception.RetryableEmailException;
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

    @Value("${app.mail.max-attempts:5}")
    private int maxAttempts;

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

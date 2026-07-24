package com.limitedgoods.limitedgoods.notification.entity;

import com.limitedgoods.limitedgoods.notification.exception.ClaimOwnershipLostException;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateData;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateKey;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "internal_email_delivery", uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
public class EmailDelivery {
    public enum Status { PENDING, PROCESSING, SENT, FAILED, DEAD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private EmailTemplateType templateType;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "claim_token", columnDefinition = "uuid")
    private UUID claimToken;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "lease_expired_count", nullable = false)
    private int leaseExpiredCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmailDelivery(
            Long eventId,
            Long orderId,
            String recipientEmail,
            EmailTemplateType templateType,
            int templateVersion
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.recipientEmail = recipientEmail;
        this.templateType = templateType;
        this.templateVersion = templateVersion;

        this.status = Status.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public EmailTemplateKey getTemplateKey() {
        return new EmailTemplateKey(templateType, templateVersion);
    }

    public EmailTemplateData getTemplateData() {
        return new EmailTemplateData(orderId);
    }

    public UUID markProcessing(LocalDateTime now) {
        if (status == Status.SENT || status == Status.DEAD) {
            throw new IllegalStateException(
                    "Completed email delivery cannot be claimed"
            );
        }

        if (status == Status.PROCESSING) {
            leaseExpiredCount++;
        }

        UUID newClaimToken = UUID.randomUUID();

        status = Status.PROCESSING;
        processingStartedAt = now;
        claimToken = newClaimToken;
        attemptCount++;

        return newClaimToken;
    }

    public boolean isOwnedBy(UUID expectedClaimToken) {
        return status == Status.PROCESSING
                && Objects.equals(claimToken, expectedClaimToken);
    }

    public void markSent(UUID expectedClaimToken, LocalDateTime now) {
        requireOwnership(expectedClaimToken);

        status = Status.SENT;
        sentAt = now;
        processingStartedAt = null;
        claimToken = null;
        lastError = null;
    }

    public void markRetryableFailure(
            UUID expectedClaimToken,
            String error,
            int maxAttempts,
            LocalDateTime now
    ) {
        requireOwnership(expectedClaimToken);

        retryCount++;
        lastError = truncateError(error);
        processingStartedAt = null;
        claimToken = null;

        if (attemptCount >= maxAttempts) {
            status = Status.DEAD;
            return;
        }

        status = Status.FAILED;

        long backoffMinutes = Math.min(
                30,
                1L << Math.min(retryCount - 1, 5)
        );

        nextAttemptAt = now.plusMinutes(backoffMinutes);
    }

    public void markPermanentFailure(
            UUID expectedClaimToken,
            String error
    ) {
        requireOwnership(expectedClaimToken);

        status = Status.DEAD;
        retryCount++;
        lastError = truncateError(error);
        processingStartedAt = null;
        claimToken = null;
    }

    public void requeueDead(LocalDateTime now) {
        if (status != Status.DEAD) {
            throw new IllegalStateException(
                    "Only DEAD delivery can be requeued"
            );
        }

        status = Status.PENDING;
        retryCount = 0;
        attemptCount = 0;
        leaseExpiredCount = 0;
        nextAttemptAt = now;
        processingStartedAt = null;
        claimToken = null;

        // 장애 원인을 확인할 수 있도록 lastError는
        // SENT가 될 때까지 유지하는 것을 권장합니다.
    }

    public void releaseAfterInfrastructureFailure(
            UUID expectedClaimToken,
            String error,
            LocalDateTime nextAttemptAt
    ) {
        requireOwnership(expectedClaimToken);

        status = Status.FAILED;
        lastError = truncateError(error);

        processingStartedAt = null;
        claimToken = null;

        // markProcessing()에서 증가한 이번 시도는
        // 이메일 자체의 실패가 아니므로 원상 복구합니다.
        attemptCount = Math.max(0, attemptCount - 1);

        // provider가 이메일을 거절한 것이 아니므로
        // retryCount는 증가시키지 않습니다.
        this.nextAttemptAt = nextAttemptAt;
    }

    private void requireOwnership(UUID expectedClaimToken) {
        if (!isOwnedBy(expectedClaimToken)) {
            throw new ClaimOwnershipLostException(id, expectedClaimToken);
        }
    }

    private String truncateError(String error) {
        String message = error == null || error.isBlank() ? "Unknown email delivery error" : error;
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}

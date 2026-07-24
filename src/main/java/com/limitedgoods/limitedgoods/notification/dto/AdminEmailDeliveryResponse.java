package com.limitedgoods.limitedgoods.notification.dto;

import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateType;

import java.time.LocalDateTime;

public record AdminEmailDeliveryResponse(
        Long id,
        Long eventId,
        Long orderId,
        String maskedRecipient,
        EmailTemplateType templateType,
        Integer templateKey,
        String status,
        int retryCount,
        int attemptCount,
        int leaseExpiredCount,
        String lastError,
        LocalDateTime nextAttemptAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static AdminEmailDeliveryResponse from(
            EmailDelivery delivery
    ) {
        return new AdminEmailDeliveryResponse(
                delivery.getId(),
                delivery.getEventId(),
                delivery.getOrderId(),
                mask(delivery.getRecipientEmail()),
                delivery.getTemplateType(),
                delivery.getTemplateVersion(),
                delivery.getStatus().name(),
                delivery.getRetryCount(),
                delivery.getAttemptCount(),
                delivery.getLeaseExpiredCount(),
                delivery.getLastError(),
                delivery.getNextAttemptAt(),
                delivery.getSentAt(),
                delivery.getCreatedAt()
        );
    }

    private static String mask(String email) {
        int separator = email.indexOf('@');

        if (separator <= 1) {
            return "***";
        }

        return email.substring(0, 1)
                + "***"
                + email.substring(separator);
    }
}
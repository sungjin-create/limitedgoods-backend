package com.limitedgoods.limitedgoods.notification.template;

public record EmailContent(
        String subject,
        String body
) {
    public EmailContent {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "Email subject is required"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                    "Email body is required"
            );
        }
    }
}
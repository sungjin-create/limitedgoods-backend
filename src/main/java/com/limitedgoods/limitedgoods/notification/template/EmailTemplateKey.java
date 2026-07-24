package com.limitedgoods.limitedgoods.notification.template;

public record EmailTemplateKey(
        EmailTemplateType type,
        int version
) {
    public EmailTemplateKey {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Template type is required"
            );
        }

        if (version < 1) {
            throw new IllegalArgumentException(
                    "Template version must be positive"
            );
        }
    }
}
package com.limitedgoods.limitedgoods.notification.template;

public class EmailTemplateNotFoundException
        extends RuntimeException {

    public EmailTemplateNotFoundException(
            EmailTemplateKey key
    ) {
        super("Email template not found: " + key);
    }
}
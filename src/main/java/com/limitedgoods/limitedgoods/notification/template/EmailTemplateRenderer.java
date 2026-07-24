package com.limitedgoods.limitedgoods.notification.template;

public interface EmailTemplateRenderer {

    EmailTemplateKey key();

    EmailContent render(EmailTemplateData data);
}
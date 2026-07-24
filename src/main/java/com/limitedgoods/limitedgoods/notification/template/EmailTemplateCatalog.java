package com.limitedgoods.limitedgoods.notification.template;

public final class EmailTemplateCatalog {

    public static final EmailTemplateKey PAYMENT_COMPLETED_V1 =
            new EmailTemplateKey(
                    EmailTemplateType.PAYMENT_COMPLETED,
                    1
            );

    private EmailTemplateCatalog() {
    }
}
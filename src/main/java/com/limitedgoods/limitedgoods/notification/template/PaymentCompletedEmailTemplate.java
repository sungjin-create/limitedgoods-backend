package com.limitedgoods.limitedgoods.notification.template;

import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedEmailTemplate
        implements EmailTemplateRenderer {

    private static final EmailTemplateKey KEY =
            new EmailTemplateKey(
                    EmailTemplateType.PAYMENT_COMPLETED,
                    1
            );

    @Override
    public EmailTemplateKey key() {
        return KEY;
    }

    @Override
    public EmailContent render(
            EmailTemplateData data
    ) {
        String subject =
                "Limited Goods 결제가 완료되었습니다.";

        String body = """
                안녕하세요.

                주문 번호 %d의 결제가 정상적으로 완료되었습니다.

                Limited Goods를 이용해 주셔서 감사합니다.
                """.formatted(data.orderId());

        return new EmailContent(subject, body);
    }
}
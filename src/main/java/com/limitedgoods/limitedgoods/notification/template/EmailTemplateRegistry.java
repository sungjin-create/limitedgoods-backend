package com.limitedgoods.limitedgoods.notification.template;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmailTemplateRegistry {

    private final Map<
            EmailTemplateKey,
            EmailTemplateRenderer
            > renderers;

    public EmailTemplateRegistry(
            List<EmailTemplateRenderer> rendererList
    ) {
        Map<EmailTemplateKey, EmailTemplateRenderer> map =
                new HashMap<>();

        for (EmailTemplateRenderer renderer : rendererList) {
            EmailTemplateRenderer duplicated =
                    map.put(renderer.key(), renderer);

            if (duplicated != null) {
                throw new IllegalStateException(
                        "Duplicated email template: "
                                + renderer.key()
                );
            }
        }

        this.renderers = Map.copyOf(map);
    }

    public EmailContent render(
            EmailTemplateKey key,
            EmailTemplateData data
    ) {
        EmailTemplateRenderer renderer =
                renderers.get(key);

        if (renderer == null) {
            throw new EmailTemplateNotFoundException(key);
        }

        return renderer.render(data);
    }
}
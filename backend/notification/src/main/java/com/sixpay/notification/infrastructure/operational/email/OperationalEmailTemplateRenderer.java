package com.sixpay.notification.infrastructure.operational.email;

import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OperationalEmailTemplateRenderer {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{([A-Za-z0-9]+)}}");

    private final OperationalNotificationTemplateCatalog catalog;

    public OperationalEmailTemplateRenderer(
            OperationalNotificationTemplateCatalog catalog
    ) {
        this.catalog = Objects.requireNonNull(
                catalog,
                "catalog"
        );
    }

    public RenderedOperationalEmail render(
            NotificationIntent notification,
            String subjectPrefix
    ) {
        Objects.requireNonNull(
                notification,
                "notification"
        );

        if (notification.channel()
                != com.sixpay.notification.domain.model
                .NotificationChannel.EMAIL) {
            throw new IllegalArgumentException(
                    "Operational email renderer only supports EMAIL"
            );
        }

        var definition =
                catalog.definition(
                        notification.templateKey()
                );

        Map<String, String> variables =
                notification.templateVariables();

        if (!definition.allowedVariables()
                .equals(variables.keySet())) {
            throw new IllegalArgumentException(
                    "Template variables do not exactly match "
                            + "the approved template contract"
            );
        }

        String body = load(
                definition.resourcePath()
        );

        for (var entry : variables.entrySet()) {
            body = body.replace(
                    "{{" + entry.getKey() + "}}",
                    sanitizeBodyValue(
                            entry.getValue()
                    )
            );
        }

        Matcher unresolved =
                PLACEHOLDER.matcher(body);

        if (unresolved.find()) {
            throw new IllegalArgumentException(
                    "Notification template contains "
                            + "an unresolved variable"
            );
        }

        return new RenderedOperationalEmail(
                subject(
                        notification.templateKey(),
                        subjectPrefix
                ),
                body.strip()
        );
    }

    private static String subject(
            NotificationTemplateKey templateKey,
            String subjectPrefix
    ) {
        String prefix =
                subjectPrefix == null
                        || subjectPrefix.isBlank()
                        ? "[SIXPAY]"
                        : subjectPrefix.strip();

        String text = switch (templateKey) {
            case PAYMENT_POSTED_ADMIN_V1 ->
                    "Paiement comptabilisé";

            case ACCOUNTING_BATCH_COMPLETED_ADMIN_V1 ->
                    "Batch comptable terminé";
        };

        return prefix
                + " "
                + text;
    }

    private static String load(
            String resourcePath
    ) {
        ClassPathResource resource =
                new ClassPathResource(
                        resourcePath
                );

        try {
            return resource.getContentAsString(
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load operational "
                            + "notification template",
                    exception
            );
        }
    }

    private static String sanitizeBodyValue(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\r", " ")
                .strip();
    }

    public record RenderedOperationalEmail(
            String subject,
            String body
    ) {
        public RenderedOperationalEmail {
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException(
                        "subject is required"
                );
            }

            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException(
                        "body is required"
                );
            }

            subject = subject.strip();
            body = body.strip();
        }
    }
}

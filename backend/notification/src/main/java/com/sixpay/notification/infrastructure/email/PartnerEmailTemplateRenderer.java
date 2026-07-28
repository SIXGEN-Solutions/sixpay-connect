package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Renders the Partner notification templates stored in the classpath.
 */
public final class PartnerEmailTemplateRenderer {

    private static final String TEMPLATE_ROOT = "templates/notification/";

    private final Map<PartnerDecisionNotification.Decision, String> templates;

    public PartnerEmailTemplateRenderer() {
        templates = new EnumMap<>(PartnerDecisionNotification.Decision.class);
        templates.put(
                PartnerDecisionNotification.Decision.APPROVED,
                load("partner-activated.html")
        );
        templates.put(
                PartnerDecisionNotification.Decision.REJECTED,
                load("partner-rejected.html")
        );
        templates.put(
                PartnerDecisionNotification.Decision.SUSPENDED,
                load("partner-suspended.html")
        );
    }

    public RenderedEmail render(PartnerDecisionNotification notification) {
        Objects.requireNonNull(notification, "notification is required");

        String subject = switch (notification.decision()) {
            case APPROVED -> "Activation de votre accès SIXPAY CONNECT";
            case REJECTED ->
                    "Décision concernant votre accès SIXPAY CONNECT";
            case SUSPENDED -> "Suspension de votre accès SIXPAY CONNECT";
        };

        String body = template(notification.decision())
                .replace(
                        "{{partnerId}}",
                        escapeHtml(notification.partnerId().toString())
                )
                .replace(
                        "{{correlationId}}",
                        escapeHtml(notification.correlationId())
                )
                .replace(
                        "{{reason}}",
                        escapeHtml(reason(notification))
                );

        return new RenderedEmail(subject, body);
    }

    private String template(
            PartnerDecisionNotification.Decision decision
    ) {
        String template = templates.get(decision);
        if (template == null) {
            throw new IllegalArgumentException(
                    "No email template configured for decision " + decision
            );
        }
        return template;
    }

    private static String reason(
            PartnerDecisionNotification notification
    ) {
        String reason = notification.reason();
        return reason == null || reason.isBlank()
                ? "Veuillez contacter l'administrateur SIXPAY."
                : reason.strip();
    }

    private static String load(String fileName) {
        ClassPathResource resource =
                new ClassPathResource(TEMPLATE_ROOT + fileName);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load notification email template: "
                            + resource.getPath(),
                    exception
            );
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record RenderedEmail(String subject, String body) {

        public RenderedEmail {
            subject = requireText(subject, "subject");
            body = requireText(body, "body");
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value.strip();
        }
    }
}

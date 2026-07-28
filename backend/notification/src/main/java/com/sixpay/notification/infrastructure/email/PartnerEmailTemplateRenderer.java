package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;

import java.util.Objects;

/**
 * Produces the minimal email representation required by the Partner vertical
 * slice. External HTML templates can replace these bodies later without
 * changing the application port or the SMTP gateway.
 */
public final class PartnerEmailTemplateRenderer {

    public RenderedEmail render(PartnerDecisionNotification notification) {
        Objects.requireNonNull(notification, "notification is required");

        return switch (notification.decision()) {
            case APPROVED -> new RenderedEmail(
                    "Activation de votre accès SIXPAY CONNECT",
                    """
                    Bonjour,

                    Votre accès partenaire SIXPAY CONNECT est maintenant actif.

                    Identifiant partenaire : %s
                    Référence de suivi : %s

                    Cordialement,
                    L'équipe SIXPAY CONNECT
                    """.formatted(
                            notification.partnerId(),
                            notification.correlationId()
                    )
            );
            case REJECTED -> new RenderedEmail(
                    "Décision concernant votre accès SIXPAY CONNECT",
                    """
                    Bonjour,

                    Votre demande d'accès partenaire SIXPAY CONNECT a été rejetée.

                    Motif : %s
                    Identifiant partenaire : %s
                    Référence de suivi : %s

                    Cordialement,
                    L'équipe SIXPAY CONNECT
                    """.formatted(
                            notification.reason(),
                            notification.partnerId(),
                            notification.correlationId()
                    )
            );
            case SUSPENDED -> new RenderedEmail(
                    "Suspension de votre accès SIXPAY CONNECT",
                    """
                    Bonjour,

                    Votre accès partenaire SIXPAY CONNECT a été suspendu.

                    Motif : %s
                    Identifiant partenaire : %s
                    Référence de suivi : %s

                    Cordialement,
                    L'équipe SIXPAY CONNECT
                    """.formatted(
                            reasonOrDefault(notification.reason()),
                            notification.partnerId(),
                            notification.correlationId()
                    )
            );
        };
    }

    private static String reasonOrDefault(String reason) {
        return reason == null || reason.isBlank()
                ? "Veuillez contacter l'administrateur SIXPAY."
                : reason.strip();
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

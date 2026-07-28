package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerEmailTemplateRendererTest {

    private final PartnerEmailTemplateRenderer renderer =
            new PartnerEmailTemplateRenderer();

    @Test
    void rendersTheActivationTemplate() {
        var rendered = renderer.render(notification(
                PartnerDecisionNotification.Decision.APPROVED,
                null
        ));

        assertThat(rendered.subject()).contains("Activation");
        assertThat(rendered.body())
                .contains("<!doctype html>")
                .contains("Votre accès est actif")
                .doesNotContain("{{partnerId}}")
                .doesNotContain("{{correlationId}}");
    }

    @Test
    void rendersTheRejectionTemplateAndEscapesItsReason() {
        var rendered = renderer.render(notification(
                PartnerDecisionNotification.Decision.REJECTED,
                "<script>alert('x')</script>"
        ));

        assertThat(rendered.subject()).contains("Décision");
        assertThat(rendered.body())
                .contains("&lt;script&gt;")
                .contains("&#39;x&#39;")
                .doesNotContain("<script>");
    }

    @Test
    void rendersTheSuspensionTemplate() {
        var rendered = renderer.render(notification(
                PartnerDecisionNotification.Decision.SUSPENDED,
                "Risque détecté"
        ));

        assertThat(rendered.subject()).contains("Suspension");
        assertThat(rendered.body())
                .contains("Votre accès a été suspendu")
                .contains("Risque détecté")
                .doesNotContain("{{reason}}");
    }

    private static PartnerDecisionNotification notification(
            PartnerDecisionNotification.Decision decision,
            String reason
    ) {
        return new PartnerDecisionNotification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alice.ops@example.com",
                decision,
                reason,
                "corr-email-test"
        );
    }
}

package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerEmailTemplateRendererTest {

    private final PartnerEmailTemplateRenderer renderer =
            new PartnerEmailTemplateRenderer();

    @Test
    void rendersApprovalRejectionAndSuspension() {
        assertThat(renderer.render(notification(
                PartnerDecisionNotification.Decision.APPROVED,
                null
        )).subject()).contains("Activation");

        assertThat(renderer.render(notification(
                PartnerDecisionNotification.Decision.REJECTED,
                "Dossier incomplet"
        )).body()).contains("Dossier incomplet");

        assertThat(renderer.render(notification(
                PartnerDecisionNotification.Decision.SUSPENDED,
                "Risque détecté"
        )).subject()).contains("Suspension");
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

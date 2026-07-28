package com.sixpay.notification.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.model.PartnerStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerDecisionNotificationServiceTest {

    private static final UUID EVENT_ID =
            UUID.fromString("19516c06-ae79-4f94-b321-66823934b9ff");
    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");
    private static final String CORRELATION_ID = "corr-decision";

    private final List<PartnerDecisionNotification> sent = new ArrayList<>();

    @Test
    void sendsApprovalToTechnicalContact() {
        var service = service("ACTIVE", null);

        service.handle(envelope(2));

        assertThat(sent).singleElement().satisfies(notification -> {
            assertThat(notification.decision())
                    .isEqualTo(PartnerDecisionNotification.Decision.APPROVED);
            assertThat(notification.recipientEmail())
                    .isEqualTo("alice.ops@example.com");
        });
    }

    @Test
    void sendsRejectionWithReasonToTechnicalContact() {
        var service = service("REJECTED", "Dossier incomplet");

        service.handle(envelope(2));

        assertThat(sent).singleElement().satisfies(notification -> {
            assertThat(notification.decision())
                    .isEqualTo(PartnerDecisionNotification.Decision.REJECTED);
            assertThat(notification.reason()).isEqualTo("Dossier incomplet");
        });
    }

    @Test
    void ignoresNonDecisionStatusChangesAndUnrelatedEvents() {
        service("SUSPENDED", "Risque détecté").handle(envelope(2));
        service("ACTIVE", null).handle(new IntegrationEventEnvelope(
                EVENT_ID,
                "PaymentCapturedIntegrationEvent",
                1,
                "PAYMENT",
                PARTNER_ID,
                CORRELATION_ID,
                Instant.parse("2026-07-27T12:00:00Z"),
                "{}"
        ));

        assertThat(sent).isEmpty();
    }

    @Test
    void rejectsUnsupportedSchemaAndEnvelopePayloadMismatch() {
        assertThatThrownBy(() -> service("ACTIVE", null).handle(envelope(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schema");

        var mismatched = new PartnerDecisionNotificationService(
                ignored -> new PartnerStatusChangedEvent(
                        2,
                        UUID.randomUUID(),
                        PARTNER_ID,
                        "PENDING_VALIDATION",
                        "ACTIVE",
                        null,
                        "alice.ops@example.com",
                        "manager@sixpay",
                        CORRELATION_ID,
                        Instant.parse("2026-07-27T12:00:00Z")
                ),
                sent::add
        );
        assertThatThrownBy(() -> mismatched.handle(envelope(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("envelope");
    }

    private PartnerDecisionNotificationService service(
            String currentStatus,
            String reason
    ) {
        return new PartnerDecisionNotificationService(
                ignored -> new PartnerStatusChangedEvent(
                        2,
                        EVENT_ID,
                        PARTNER_ID,
                        "PENDING_VALIDATION",
                        currentStatus,
                        reason,
                        "alice.ops@example.com",
                        "manager@sixpay",
                        CORRELATION_ID,
                        Instant.parse("2026-07-27T12:00:00Z")
                ),
                sent::add
        );
    }

    private static IntegrationEventEnvelope envelope(int schemaVersion) {
        return new IntegrationEventEnvelope(
                EVENT_ID,
                "PartnerStatusChangedIntegrationEvent",
                schemaVersion,
                "PARTNER",
                PARTNER_ID,
                CORRELATION_ID,
                Instant.parse("2026-07-27T12:00:00Z"),
                "{\"eventId\":\"" + EVENT_ID + "\"}"
        );
    }
}

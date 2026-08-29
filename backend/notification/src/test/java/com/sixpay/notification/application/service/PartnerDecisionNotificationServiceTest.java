package com.sixpay.notification.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.model.PartnerStatusChangedEvent;
import com.sixpay.notification.application.port.output.NotificationDeliveryStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final Instant NOW =
            Instant.parse("2026-07-27T12:00:00Z");

    private final List<PartnerDecisionNotification> sent = new ArrayList<>();
    private final InMemoryDeliveryStore deliveryStore =
            new InMemoryDeliveryStore();

    @Test
    void sendsApprovalAndMarksTheDeliveryAsSent() {
        var service = service("ACTIVE", null);

        service.handle(envelope(2));

        assertThat(sent).singleElement().satisfies(notification -> {
            assertThat(notification.decision())
                    .isEqualTo(PartnerDecisionNotification.Decision.APPROVED);
            assertThat(notification.recipientEmail())
                    .isEqualTo("alice.ops@example.com");
        });
        assertThat(deliveryStore.registration.template())
                .isEqualTo("partner-activated");
        assertThat(deliveryStore.sentEventId).isEqualTo(EVENT_ID);
    }

    @Test
    void sendsRejectionAndSuspensionWithTheirTemplates() {
        service("REJECTED", "Dossier incomplet").handle(envelope(2));

        assertThat(deliveryStore.registration.template())
                .isEqualTo("partner-rejected");
        assertThat(sent).singleElement().satisfies(notification ->
                assertThat(notification.reason())
                        .isEqualTo("Dossier incomplet")
        );

        sent.clear();
        deliveryStore.reset();
        service("SUSPENDED", "Risque détecté").handle(envelope(2));

        assertThat(deliveryStore.registration.template())
                .isEqualTo("partner-suspended");
        assertThat(sent).singleElement().satisfies(notification ->
                assertThat(notification.decision())
                        .isEqualTo(
                                PartnerDecisionNotification.Decision.SUSPENDED
                        )
        );
    }

    @Test
    void ignoresAnAlreadyRegisteredEventWithoutSendingAgain() {
        var service = service("ACTIVE", null);

        service.handle(envelope(2));
        service.handle(envelope(2));

        assertThat(sent).hasSize(1);
        assertThat(deliveryStore.startCalls).isEqualTo(2);
        assertThat(deliveryStore.sentCalls).isEqualTo(1);
    }

    @Test
    void recordsTheFailureAndRethrowsTheSendingError() {
        var service = new PartnerDecisionNotificationService(
                ignored -> partnerEvent("ACTIVE", null),
                ignored -> {
                    throw new IllegalStateException("SMTP unavailable");
                },
                deliveryStore,
                () -> NOW,
                NotificationRetryPolicy.defaults()
        );

        assertThatThrownBy(() -> service.handle(envelope(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SMTP unavailable");

        assertThat(deliveryStore.failedEventId).isEqualTo(EVENT_ID);
        assertThat(deliveryStore.lastError).isEqualTo("SMTP unavailable");
        assertThat(deliveryStore.nextAttemptAt)
                .isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void ignoresNonNotifiableStatusChangesAndUnrelatedEvents() {
        service("PENDING_VALIDATION", null).handle(envelope(2));

        service("ACTIVE", null).handle(new IntegrationEventEnvelope(
                EVENT_ID,
                "PaymentCapturedIntegrationEvent",
                1,
                "PAYMENT",
                PARTNER_ID,
                CORRELATION_ID,
                NOW,
                "{}"
        ));

        assertThat(sent).isEmpty();
        assertThat(deliveryStore.registration).isNull();
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
                        NOW
                ),
                sent::add,
                deliveryStore,
                () -> NOW,
                NotificationRetryPolicy.defaults()
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
                ignored -> partnerEvent(currentStatus, reason),
                sent::add,
                deliveryStore,
                () -> NOW,
                NotificationRetryPolicy.defaults()
        );
    }

    private static PartnerStatusChangedEvent partnerEvent(
            String currentStatus,
            String reason
    ) {
        return new PartnerStatusChangedEvent(
                2,
                EVENT_ID,
                PARTNER_ID,
                "PENDING_VALIDATION",
                currentStatus,
                reason,
                "alice.ops@example.com",
                "manager@sixpay",
                CORRELATION_ID,
                NOW
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
                NOW,
                "{\"eventId\":\"" + EVENT_ID + "\"}"
        );
    }

    private static final class InMemoryDeliveryStore
            implements NotificationDeliveryStore {

        private NotificationDeliveryRegistration registration;
        private UUID sentEventId;
        private UUID failedEventId;
        private String lastError;
        private Instant nextAttemptAt;
        private int startCalls;
        private int sentCalls;

        @Override
        public boolean tryStart(
                NotificationDeliveryRegistration candidate
        ) {
            startCalls++;
            if (registration != null
                    && registration.eventId().equals(candidate.eventId())) {
                return false;
            }
            registration = candidate;
            return true;
        }

        @Override
        public List<com.sixpay.notification.application.model.NotificationDeliveryAttempt>
        claimDue(Instant now, int batchSize) {
            return Collections.emptyList();
        }

        @Override
        public void markSent(UUID eventId, Instant sentAt) {
            sentCalls++;
            sentEventId = eventId;
        }

        @Override
        public void markFailed(
                UUID eventId,
                String error,
                Instant failedAt,
                Instant retryAt
        ) {
            failedEventId = eventId;
            lastError = error;
            nextAttemptAt = retryAt;
        }

        @Override
        public void markDead(
                UUID eventId,
                String error,
                Instant failedAt
        ) {
            failedEventId = eventId;
            lastError = error;
        }

        private void reset() {
            registration = null;
            sentEventId = null;
            failedEventId = null;
            lastError = null;
            nextAttemptAt = null;
            startCalls = 0;
            sentCalls = 0;
        }
    }
}

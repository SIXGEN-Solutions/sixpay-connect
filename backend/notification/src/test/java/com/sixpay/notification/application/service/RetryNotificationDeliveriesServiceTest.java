package com.sixpay.notification.application.service;

import com.sixpay.notification.application.model.NotificationDeliveryAttempt;
import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetryNotificationDeliveriesServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final Instant NOW =
            Instant.parse("2026-07-28T12:00:00Z");

    private final RecordingStore store = new RecordingStore();
    private final List<PartnerDecisionNotification> sent =
            new ArrayList<>();
    private final NotificationRetryPolicy policy =
            new NotificationRetryPolicy(
                    3,
                    Duration.ofMinutes(1),
                    2.0,
                    Duration.ofMinutes(10)
            );

    @Test
    void sendsDueDeliveryAndMarksItSent() {
        store.due = List.of(attempt(2));
        var service = service(sent::add);

        service.retryDueDeliveries();

        assertThat(sent).singleElement().satisfies(notification -> {
            assertThat(notification.eventId()).isEqualTo(EVENT_ID);
            assertThat(notification.reason()).isEqualTo("Dossier incomplet");
        });
        assertThat(store.sentEventId).isEqualTo(EVENT_ID);
        assertThat(store.failedEventId).isNull();
    }

    @Test
    void reschedulesFailureWithBackoffBeforeMaximum() {
        store.due = List.of(attempt(2));
        var service = service(ignored -> {
            throw new IllegalStateException("SMTP unavailable");
        });

        service.retryDueDeliveries();

        assertThat(store.failedEventId).isEqualTo(EVENT_ID);
        assertThat(store.deadEventId).isNull();
        assertThat(store.nextAttemptAt)
                .isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void marksDeliveryDeadAfterMaximumAttempt() {
        store.due = List.of(attempt(3));
        var service = service(ignored -> {
            throw new IllegalStateException("SMTP unavailable");
        });

        service.retryDueDeliveries();

        assertThat(store.deadEventId).isEqualTo(EVENT_ID);
        assertThat(store.failedEventId).isNull();
    }

    private RetryNotificationDeliveriesService service(
            com.sixpay.notification.application.port.out.PartnerNotificationSender
                    sender
    ) {
        return new RetryNotificationDeliveriesService(
                store,
                sender,
                () -> NOW,
                policy,
                20
        );
    }

    private static NotificationDeliveryAttempt attempt(int count) {
        return new NotificationDeliveryAttempt(
                EVENT_ID,
                PARTNER_ID,
                "alice.ops@example.com",
                "partner-rejected",
                "Dossier incomplet",
                "corr-retry",
                count
        );
    }

    private static final class RecordingStore
            implements NotificationDeliveryStore {

        private List<NotificationDeliveryAttempt> due = List.of();
        private UUID sentEventId;
        private UUID failedEventId;
        private UUID deadEventId;
        private Instant nextAttemptAt;

        @Override
        public boolean tryStart(
                NotificationDeliveryRegistration registration
        ) {
            return false;
        }

        @Override
        public List<NotificationDeliveryAttempt> claimDue(
                Instant now,
                int batchSize
        ) {
            return due;
        }

        @Override
        public void markSent(UUID eventId, Instant sentAt) {
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
            nextAttemptAt = retryAt;
        }

        @Override
        public void markDead(
                UUID eventId,
                String error,
                Instant failedAt
        ) {
            deadEventId = eventId;
        }
    }
}

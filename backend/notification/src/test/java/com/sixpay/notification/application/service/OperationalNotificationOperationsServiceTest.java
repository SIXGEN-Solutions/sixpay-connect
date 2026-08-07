package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.input.OperationalNotificationReplayCommand;
import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.domain.model.*;
import com.sixpay.notification.domain.repository.*;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OperationalNotificationOperationsServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-07T17:00:00Z");

    @Test
    void deadLetterReplayPreservesIdentityAndCreatesAudit() {
        FakePersistence persistence =
                new FakePersistence(deadLettered());

        AtomicInteger replayMetrics =
                new AtomicInteger();

        OperationalNotificationOperationsTelemetry telemetry =
                new OperationalNotificationOperationsTelemetry() {
                    @Override
                    public void recordReplay() {
                        replayMetrics.incrementAndGet();
                    }

                    @Override
                    public void recordPurged(int count) {
                    }
                };

        var service = new OperationalNotificationOperationsService(
                persistence,
                persistence,
                persistence,
                persistence,
                () -> UUID.fromString(
                        "57be3d9a-fb07-4ee2-a4d1-76038c38bfb0"
                ),
                telemetry,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        UUID originalId =
                persistence.delivery.intent().notificationId();

        String originalDedup =
                persistence.delivery.intent()
                        .deduplicationKey()
                        .value();

        var result = service.replay(
                originalId,
                new OperationalNotificationReplayCommand(
                        "ops-user-42",
                        "SMTP configuration corrected"
                )
        );

        assertEquals(originalId, result.notificationId());
        assertEquals(
                NotificationDeliveryStatus.FAILED_RETRYABLE,
                result.status()
        );
        assertEquals(1, result.replayCount());

        assertEquals(
                originalId,
                persistence.delivery.intent().notificationId()
        );
        assertEquals(
                originalDedup,
                persistence.delivery.intent()
                        .deduplicationKey()
                        .value()
        );
        assertEquals(5, persistence.delivery.attemptCount());
        assertEquals(0, persistence.delivery.cycleAttemptCount());
        assertEquals(NOW, persistence.delivery.nextAttemptAt());

        assertEquals(1, persistence.replays.size());
        NotificationReplayAudit audit =
                persistence.replays.getFirst();
        assertEquals("ops-user-42", audit.operatorReference());
        assertEquals(
                "SMTP configuration corrected",
                audit.reason()
        );
        assertEquals(
                NotificationDeliveryStatus.DEAD_LETTERED,
                audit.previousStatus()
        );

        assertEquals(1, replayMetrics.get());
    }

    @Test
    void nonDeadLetterCannotBeReplayed() {
        FakePersistence persistence =
                new FakePersistence(
                        OperationalNotificationDelivery.pending(
                                intent(
                                        NotificationDeliveryStatus.PENDING
                                )
                        )
                );

        var service = service(persistence);

        assertThrows(
                IllegalStateException.class,
                () -> service.replay(
                        persistence.delivery.intent()
                                .notificationId(),
                        new OperationalNotificationReplayCommand(
                                "ops-user-42",
                                "manual retry"
                        )
                )
        );

        assertTrue(persistence.replays.isEmpty());
    }

    @Test
    void statusNeverContainsRawEmailAddress() {
        FakePersistence persistence =
                new FakePersistence(deadLettered());

        var status = service(persistence)
                .status(
                        persistence.delivery.intent()
                                .notificationId()
                )
                .orElseThrow();

        assertEquals(
                "operations-admin",
                status.recipientReference()
        );

        assertFalse(
                status.recipientReference().contains("@")
        );
    }

    private static OperationalNotificationOperationsService service(
            FakePersistence persistence
    ) {
        return new OperationalNotificationOperationsService(
                persistence,
                persistence,
                persistence,
                persistence,
                UUID::randomUUID,
                OperationalNotificationOperationsTelemetry.NOOP,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static OperationalNotificationDelivery deadLettered() {
        return new OperationalNotificationDelivery(
                intent(NotificationDeliveryStatus.DEAD_LETTERED),
                5,
                5,
                0,
                null,
                NOW.minusSeconds(30),
                null,
                null,
                "SMTP_SEND_FAILED",
                null
        );
    }

    private static NotificationIntent intent(
            NotificationDeliveryStatus status
    ) {
        return new NotificationIntent(
                UUID.fromString(
                        "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
                ),
                new NotificationSourceReference(
                        OperationalNotificationTriggerType.PAYMENT_POSTED,
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                new NotificationRecipient(
                        NotificationRecipientType.SIXPAY_ADMIN,
                        "operations-admin",
                        Locale.FRENCH
                ),
                NotificationChannel.EMAIL,
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1,
                new NotificationDeduplicationKey(
                        "a".repeat(64)
                ),
                Map.of(
                        "paymentReference",
                        "PAY-20260807-0001"
                ),
                status,
                NOW.minusSeconds(3600),
                "corr-ops-5.7.4"
        );
    }

    private static final class FakePersistence
            implements OperationalNotificationRepository,
            OperationalNotificationOperationsRepository,
            NotificationAttemptRepository,
            NotificationReplayRepository {

        private OperationalNotificationDelivery delivery;
        private final List<NotificationReplayAudit> replays =
                new ArrayList<>();

        private FakePersistence(
                OperationalNotificationDelivery delivery
        ) {
            this.delivery = delivery;
        }

        @Override
        public NotificationSaveResult saveIfAbsent(
                OperationalNotificationDelivery delivery
        ) {
            return new NotificationSaveResult(
                    this.delivery,
                    false
            );
        }

        @Override
        public OperationalNotificationDelivery save(
                OperationalNotificationDelivery delivery
        ) {
            this.delivery = delivery;
            return delivery;
        }

        @Override
        public Optional<OperationalNotificationDelivery> findById(
                UUID notificationId
        ) {
            return delivery.intent()
                    .notificationId()
                    .equals(notificationId)
                    ? Optional.of(delivery)
                    : Optional.empty();
        }

        @Override
        public Optional<OperationalNotificationDelivery>
        findByDeduplicationKey(
                NotificationDeduplicationKey key
        ) {
            return Optional.of(delivery);
        }

        @Override
        public List<UUID> findDueNotificationIds(
                Instant dueAt,
                int limit
        ) {
            return List.of();
        }

        @Override
        public Optional<OperationalNotificationDelivery> claimForDispatch(
                UUID notificationId,
                Instant claimedAt
        ) {
            return Optional.empty();
        }

        @Override
        public NotificationAttempt append(
                NotificationAttempt attempt
        ) {
            return attempt;
        }

        @Override
        public List<NotificationAttempt> findByNotificationId(
                UUID notificationId
        ) {
            return List.of();
        }

        @Override
        public List<UUID> findIdsByStatus(
                NotificationDeliveryStatus status,
                int limit
        ) {
            return delivery.intent().status() == status
                    ? List.of(
                            delivery.intent().notificationId()
                    )
                    : List.of();
        }

        @Override
        public long countByStatus(
                NotificationDeliveryStatus status
        ) {
            return delivery.intent().status() == status
                    ? 1
                    : 0;
        }

        @Override
        public long countDue(
                Instant dueAt
        ) {
            return 0;
        }

        @Override
        public Optional<Instant> findOldestDueAt(
                Instant dueAt
        ) {
            return Optional.empty();
        }

        @Override
        public int purgeTerminal(
                Instant deliveredBefore,
                Instant failedBefore,
                int limit
        ) {
            return 0;
        }

        @Override
        public Optional<OperationalNotificationDelivery> replayDeadLetter(
                NotificationReplayAudit audit
        ) {
            if (delivery.intent().status()
                    != NotificationDeliveryStatus.DEAD_LETTERED) {
                return Optional.empty();
            }

            delivery = delivery.replayed(
                    audit.requestedAt()
            );
            replays.add(audit);
            return Optional.of(delivery);
        }

        @Override
        public List<NotificationReplayAudit> findReplaysByNotificationId(
                UUID notificationId
        ) {
            return List.copyOf(replays);
        }
    }
}

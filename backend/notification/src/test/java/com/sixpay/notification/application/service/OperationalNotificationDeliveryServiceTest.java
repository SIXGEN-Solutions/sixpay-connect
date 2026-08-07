package com.sixpay.notification.application.service;

import com.sixpay.notification.application.exception.PermanentNotificationDeliveryException;
import com.sixpay.notification.application.exception.RetryableNotificationDeliveryException;
import com.sixpay.notification.application.port.output.NotificationDispatchResult;
import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;
import com.sixpay.notification.domain.policy.NotificationDeliveryLifecycle;
import com.sixpay.notification.domain.policy.OperationalNotificationRetryPolicy;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.NotificationSaveResult;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalNotificationDeliveryServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-07T16:00:00Z");

    @Test
    void retryableFailureIsScheduledThenDeadLettered() {
        FakePersistence persistence =
                new FakePersistence(pending());

        AtomicInteger calls = new AtomicInteger();

        var service = service(
                persistence,
                notification -> {
                    calls.incrementAndGet();
                    throw new RetryableNotificationDeliveryException(
                            "SMTP_TEMPORARY",
                            "temporary",
                            null
                    );
                },
                2
        );

        var first = service.processDue(
                NOW,
                10
        );

        assertEquals(1, first.retryScheduled());
        assertEquals(
                NotificationDeliveryStatus.FAILED_RETRYABLE,
                persistence.delivery.intent().status()
        );
        assertEquals(1, persistence.delivery.attemptCount());

        var second = service.processDue(
                NOW.plusSeconds(60),
                10
        );

        assertEquals(1, second.deadLettered());
        assertEquals(
                NotificationDeliveryStatus.DEAD_LETTERED,
                persistence.delivery.intent().status()
        );
        assertEquals(2, persistence.delivery.attemptCount());
        assertEquals(2, calls.get());
        assertEquals(2, persistence.attempts.size());
    }

    @Test
    void permanentFailureIsNeverRetried() {
        FakePersistence persistence =
                new FakePersistence(pending());

        var service = service(
                persistence,
                notification -> {
                    throw new PermanentNotificationDeliveryException(
                            "INVALID_RECIPIENT",
                            "invalid recipient",
                            null
                    );
                },
                5
        );

        var report = service.processDue(
                NOW,
                10
        );

        assertEquals(1, report.permanentlyFailed());
        assertEquals(
                NotificationDeliveryStatus.FAILED_PERMANENT,
                persistence.delivery.intent().status()
        );

        var second = service.processDue(
                NOW.plusSeconds(3600),
                10
        );

        assertEquals(0, second.candidates());
    }

    @Test
    void deliveredResultCompletesDelivery() {
        FakePersistence persistence =
                new FakePersistence(pending());

        var service = service(
                persistence,
                notification ->
                        NotificationDispatchResult.delivered(
                                "mail-provider-42"
                        ),
                5
        );

        var report = service.processDue(
                NOW,
                10
        );

        assertEquals(1, report.delivered());
        assertEquals(
                NotificationDeliveryStatus.DELIVERED,
                persistence.delivery.intent().status()
        );
        assertEquals(
                "mail-provider-42",
                persistence.delivery.providerReference()
        );
    }

    private static OperationalNotificationDeliveryService service(
            FakePersistence persistence,
            com.sixpay.notification.application.port.output
                    .OperationalNotificationDeliveryGateway gateway,
            int maxAttempts
    ) {
        AtomicInteger ids = new AtomicInteger();

        return new OperationalNotificationDeliveryService(
                persistence,
                persistence,
                gateway,
                () -> new UUID(
                        0L,
                        ids.incrementAndGet()
                ),
                new NotificationDeliveryLifecycle(),
                new OperationalNotificationRetryPolicy(
                        maxAttempts,
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(5)
                ),
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                )
        );
    }

    private static OperationalNotificationDelivery pending() {
        return OperationalNotificationDelivery.pending(
                new NotificationIntent(
                        UUID.fromString(
                                "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
                        ),
                        new NotificationSourceReference(
                                OperationalNotificationTriggerType
                                        .PAYMENT_POSTED,
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        ),
                        new NotificationRecipient(
                                NotificationRecipientType.SIXPAY_ADMIN,
                                "operations-admin",
                                Locale.FRENCH
                        ),
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey
                                .PAYMENT_POSTED_ADMIN_V1,
                        new NotificationDeduplicationKey(
                                "a".repeat(64)
                        ),
                        Map.of(
                                "paymentReference",
                                "PAY-20260807-0001"
                        ),
                        NotificationDeliveryStatus.PENDING,
                        NOW,
                        "corr-notification-delivery-1"
                )
        );
    }

    private static final class FakePersistence
            implements OperationalNotificationRepository,
            NotificationAttemptRepository {

        private OperationalNotificationDelivery delivery;

        private final Map<UUID, NotificationAttempt> attempts =
                new LinkedHashMap<>();

        private FakePersistence(
                OperationalNotificationDelivery delivery
        ) {
            this.delivery = delivery;
        }

        @Override
        public NotificationSaveResult saveIfAbsent(
                OperationalNotificationDelivery value
        ) {
            return new NotificationSaveResult(
                    delivery,
                    false
            );
        }

        @Override
        public OperationalNotificationDelivery save(
                OperationalNotificationDelivery value
        ) {
            delivery = value;
            return value;
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
                NotificationDeduplicationKey deduplicationKey
        ) {
            return delivery.intent()
                    .deduplicationKey()
                    .equals(deduplicationKey)
                    ? Optional.of(delivery)
                    : Optional.empty();
        }

        @Override
        public List<UUID> findDueNotificationIds(
                Instant dueAt,
                int limit
        ) {
            if ((delivery.intent().status()
                    == NotificationDeliveryStatus.PENDING
                    || delivery.intent().status()
                    == NotificationDeliveryStatus.FAILED_RETRYABLE)
                    && delivery.nextAttemptAt() != null
                    && !delivery.nextAttemptAt().isAfter(dueAt)) {
                return List.of(
                        delivery.intent().notificationId()
                );
            }

            return List.of();
        }

        @Override
        public Optional<OperationalNotificationDelivery>
        claimForDispatch(
                UUID notificationId,
                Instant claimedAt
        ) {
            if (!findDueNotificationIds(
                    claimedAt,
                    1
            ).contains(notificationId)) {
                return Optional.empty();
            }

            delivery = delivery.dispatching(
                    claimedAt
            );

            return Optional.of(delivery);
        }

        @Override
        public NotificationAttempt append(
                NotificationAttempt attempt
        ) {
            attempts.put(
                    attempt.attemptId(),
                    attempt
            );
            return attempt;
        }

        @Override
        public List<NotificationAttempt> findByNotificationId(
                UUID notificationId
        ) {
            return new ArrayList<>(
                    attempts.values()
            );
        }
    }
}

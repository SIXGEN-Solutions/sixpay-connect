package com.sixpay.notification.application.service;

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
import com.sixpay.notification.domain.repository.NotificationSaveResult;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationOrchestrationServiceTest {

    @Test
    void duplicateTriggerCreatesOnlyOneFunctionalNotification() {
        FakeRepository repository = new FakeRepository();

        var planner = new FixedPlanner(
                intent(
                        UUID.fromString(
                                "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
                        )
                )
        );

        var service =
                new OperationalNotificationOrchestrationService(
                        planner,
                        repository
                );

        var first = service.accept(nullSafeTrigger());
        var second = service.accept(nullSafeTrigger());

        assertTrue(first.successful());
        assertTrue(second.successful());
        assertEquals(1, first.persisted());
        assertEquals(0, second.persisted());
        assertEquals(1, repository.byKey.size());
    }

    @Test
    void persistenceFailureNeverEscapesToSourceTransaction() {
        var service =
                new OperationalNotificationOrchestrationService(
                        new FixedPlanner(
                                intent(
                                        UUID.fromString(
                                                "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
                                        )
                                )
                        ),
                        new FailingRepository()
                );

        var result = service.accept(
                nullSafeTrigger()
        );

        assertFalse(result.successful());
        assertEquals(
                "NOTIFICATION_REGISTRATION_FAILED",
                result.errorCode()
        );
    }

    private static NotificationIntent intent(
            UUID notificationId
    ) {
        return new NotificationIntent(
                notificationId,
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
                Instant.parse("2026-08-07T16:00:00Z"),
                "corr-notification-1"
        );
    }

    private static com.sixpay.notification.domain.model
            .OperationalNotificationTrigger nullSafeTrigger() {
        return new com.sixpay.notification.domain.model
                .PaymentPostedNotificationTrigger(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                "PAY-20260807-0001",
                "TRESORPAY",
                new java.math.BigDecimal("10000"),
                java.util.Currency.getInstance("XAF"),
                Instant.parse("2026-08-07T15:55:00Z"),
                "corr-notification-1"
        );
    }

    private record FixedPlanner(
            NotificationIntent notification
    ) implements
            com.sixpay.notification.application.port.input
                    .OperationalNotificationTriggerUseCase {

        @Override
        public List<NotificationIntent> plan(
                com.sixpay.notification.domain.model
                        .OperationalNotificationTrigger trigger
        ) {
            return List.of(notification);
        }
    }

    private static class FakeRepository
            implements OperationalNotificationRepository {

        private final Map<
                NotificationDeduplicationKey,
                OperationalNotificationDelivery
                > byKey = new LinkedHashMap<>();

        @Override
        public NotificationSaveResult saveIfAbsent(
                OperationalNotificationDelivery delivery
        ) {
            var key = delivery.intent()
                    .deduplicationKey();

            var existing = byKey.get(key);

            if (existing != null) {
                return new NotificationSaveResult(
                        existing,
                        false
                );
            }

            byKey.put(key, delivery);

            return new NotificationSaveResult(
                    delivery,
                    true
            );
        }

        @Override
        public OperationalNotificationDelivery save(
                OperationalNotificationDelivery delivery
        ) {
            byKey.put(
                    delivery.intent().deduplicationKey(),
                    delivery
            );
            return delivery;
        }

        @Override
        public Optional<OperationalNotificationDelivery> findById(
                UUID notificationId
        ) {
            return byKey.values().stream()
                    .filter(value ->
                            value.intent()
                                    .notificationId()
                                    .equals(notificationId)
                    )
                    .findFirst();
        }

        @Override
        public Optional<OperationalNotificationDelivery>
        findByDeduplicationKey(
                NotificationDeduplicationKey key
        ) {
            return Optional.ofNullable(
                    byKey.get(key)
            );
        }

        @Override
        public List<UUID> findDueNotificationIds(
                Instant dueAt,
                int limit
        ) {
            return List.of();
        }

        @Override
        public Optional<OperationalNotificationDelivery>
        claimForDispatch(
                UUID notificationId,
                Instant claimedAt
        ) {
            return Optional.empty();
        }
    }

    private static final class FailingRepository
            extends FakeRepository {

        @Override
        public NotificationSaveResult saveIfAbsent(
                OperationalNotificationDelivery delivery
        ) {
            throw new IllegalStateException(
                    "database unavailable"
            );
        }
    }
}

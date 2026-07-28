package com.sixpay.notification.infrastructure.persistence;

import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class JpaNotificationDeliveryStore
        implements NotificationDeliveryStore {

    private final NotificationDeliverySpringDataRepository repository;

    public JpaNotificationDeliveryStore(
            NotificationDeliverySpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional
    public boolean tryStart(NotificationDeliveryRegistration registration) {
        Objects.requireNonNull(registration, "registration is required");
        return repository.insertProcessing(
                UUID.randomUUID(),
                registration.eventId(),
                registration.aggregateId(),
                registration.eventType(),
                registration.recipient(),
                registration.template(),
                registration.createdAt(),
                registration.correlationId()
        ) == 1;
    }

    @Override
    @Transactional
    public void markSent(UUID eventId, Instant sentAt) {
        requireUpdated(
                repository.markSent(
                        Objects.requireNonNull(eventId),
                        Objects.requireNonNull(sentAt)
                ),
                eventId,
                "SENT"
        );
    }

    @Override
    @Transactional
    public void markFailed(
            UUID eventId,
            String error,
            Instant failedAt,
            Instant nextAttemptAt
    ) {
        Objects.requireNonNull(failedAt);
        requireUpdated(
                repository.markFailed(
                        Objects.requireNonNull(eventId),
                        requireText(error, "error"),
                        Objects.requireNonNull(nextAttemptAt)
                ),
                eventId,
                "FAILED"
        );
    }

    private static void requireUpdated(
            int updated,
            UUID eventId,
            String targetStatus
    ) {
        if (updated != 1) {
            throw new IllegalStateException(
                    "Cannot mark notification delivery "
                            + eventId + " as " + targetStatus
            );
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.length() <= 2000
                ? value.strip()
                : value.substring(0, 2000);
    }
}

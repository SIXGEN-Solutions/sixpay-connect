package com.sixpay.notification.infrastructure.persistence;

import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.model.NotificationDeliveryAttempt;
import com.sixpay.notification.application.port.output.NotificationDeliveryStore;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.List;
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
                registration.reason(),
                registration.createdAt(),
                registration.correlationId()
        ) == 1;
    }

    @Override
    @Transactional
    public List<NotificationDeliveryAttempt> claimDue(
            Instant now,
            int batchSize
    ) {
        Objects.requireNonNull(now, "now is required");
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }
        var entities = repository.lockDue(now, batchSize);
        return entities.stream()
                .map(entity -> {
                    entity.claimForRetry();
                    return new NotificationDeliveryAttempt(
                            entity.eventId(),
                            entity.aggregateId(),
                            entity.recipient(),
                            entity.template(),
                            entity.reason(),
                            entity.correlationId(),
                            entity.attemptCount()
                    );
                })
                .toList();
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

    @Override
    @Transactional
    public void markDead(
            UUID eventId,
            String error,
            Instant failedAt
    ) {
        Objects.requireNonNull(failedAt, "failedAt is required");
        requireUpdated(
                repository.markDead(
                        Objects.requireNonNull(eventId),
                        requireText(error, "error")
                ),
                eventId,
                "DEAD"
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

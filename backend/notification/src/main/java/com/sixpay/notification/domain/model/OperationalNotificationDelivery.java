package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

public record OperationalNotificationDelivery(
        NotificationIntent intent,
        int attemptCount,
        int cycleAttemptCount,
        int replayCount,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant deliveredAt,
        Instant lastReplayAt,
        String lastErrorCode,
        String providerReference
) {
    public OperationalNotificationDelivery {
        intent = Objects.requireNonNull(
                intent,
                "intent"
        );

        if (attemptCount < 0) {
            throw new IllegalArgumentException(
                    "attemptCount must be >= 0"
            );
        }

        if (cycleAttemptCount < 0) {
            throw new IllegalArgumentException(
                    "cycleAttemptCount must be >= 0"
            );
        }

        if (cycleAttemptCount > attemptCount) {
            throw new IllegalArgumentException(
                    "cycleAttemptCount cannot exceed attemptCount"
            );
        }

        if (replayCount < 0) {
            throw new IllegalArgumentException(
                    "replayCount must be >= 0"
            );
        }

        lastErrorCode = optional(lastErrorCode);
        providerReference = optional(providerReference);

        if (intent.status()
                == NotificationDeliveryStatus.DELIVERED
                && deliveredAt == null) {
            throw new IllegalArgumentException(
                    "DELIVERED notification requires deliveredAt"
            );
        }
    }

    public static OperationalNotificationDelivery pending(
            NotificationIntent intent
    ) {
        if (intent.status()
                != NotificationDeliveryStatus.PENDING) {
            throw new IllegalArgumentException(
                    "New notification intent must be PENDING"
            );
        }

        return new OperationalNotificationDelivery(
                intent,
                0,
                0,
                0,
                intent.createdAt(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public OperationalNotificationDelivery dispatching(
            Instant at
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.DISPATCHING
                ),
                attemptCount + 1,
                cycleAttemptCount + 1,
                replayCount,
                null,
                Objects.requireNonNull(at, "at"),
                deliveredAt,
                lastReplayAt,
                null,
                providerReference
        );
    }

    public OperationalNotificationDelivery accepted(
            String externalReference
    ) {
        return copy(
                NotificationDeliveryStatus.ACCEPTED,
                null,
                deliveredAt,
                null,
                externalReference
        );
    }

    public OperationalNotificationDelivery delivered(
            Instant at,
            String externalReference
    ) {
        return copy(
                NotificationDeliveryStatus.DELIVERED,
                null,
                Objects.requireNonNull(at, "at"),
                null,
                externalReference
        );
    }

    public OperationalNotificationDelivery retryableFailure(
            Instant nextAttempt,
            String errorCode
    ) {
        return copy(
                NotificationDeliveryStatus.FAILED_RETRYABLE,
                Objects.requireNonNull(
                        nextAttempt,
                        "nextAttempt"
                ),
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
        );
    }

    public OperationalNotificationDelivery permanentFailure(
            String errorCode
    ) {
        return copy(
                NotificationDeliveryStatus.FAILED_PERMANENT,
                null,
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
        );
    }

    public OperationalNotificationDelivery deadLetter(
            String errorCode
    ) {
        return copy(
                NotificationDeliveryStatus.DEAD_LETTERED,
                null,
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
        );
    }

    public OperationalNotificationDelivery replayed(
            Instant replayedAt
    ) {
        if (intent.status()
                != NotificationDeliveryStatus.DEAD_LETTERED) {
            throw new IllegalStateException(
                    "Only DEAD_LETTERED notifications can be replayed"
            );
        }

        Instant at = Objects.requireNonNull(
                replayedAt,
                "replayedAt"
        );

        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.FAILED_RETRYABLE
                ),
                attemptCount,
                0,
                replayCount + 1,
                at,
                lastAttemptAt,
                deliveredAt,
                at,
                null,
                providerReference
        );
    }

    private OperationalNotificationDelivery copy(
            NotificationDeliveryStatus status,
            Instant nextAttempt,
            Instant delivered,
            String errorCode,
            String externalReference
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(status),
                attemptCount,
                cycleAttemptCount,
                replayCount,
                nextAttempt,
                lastAttemptAt,
                delivered,
                lastReplayAt,
                errorCode,
                externalReference
        );
    }

    private static String requiredErrorCode(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "UNCLASSIFIED_DELIVERY_FAILURE";
        }

        return value.strip();
    }

    private static String optional(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}

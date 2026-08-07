package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

public record OperationalNotificationDelivery(
        NotificationIntent intent,
        int attemptCount,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant deliveredAt,
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
                intent.createdAt(),
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
                null,
                Objects.requireNonNull(at, "at"),
                deliveredAt,
                null,
                providerReference
        );
    }

    public OperationalNotificationDelivery accepted(
            String externalReference
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.ACCEPTED
                ),
                attemptCount,
                null,
                lastAttemptAt,
                deliveredAt,
                null,
                externalReference
        );
    }

    public OperationalNotificationDelivery delivered(
            Instant at,
            String externalReference
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.DELIVERED
                ),
                attemptCount,
                null,
                lastAttemptAt,
                Objects.requireNonNull(at, "at"),
                null,
                externalReference
        );
    }

    public OperationalNotificationDelivery retryableFailure(
            Instant nextAttempt,
            String errorCode
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.FAILED_RETRYABLE
                ),
                attemptCount,
                Objects.requireNonNull(
                        nextAttempt,
                        "nextAttempt"
                ),
                lastAttemptAt,
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
        );
    }

    public OperationalNotificationDelivery permanentFailure(
            String errorCode
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.FAILED_PERMANENT
                ),
                attemptCount,
                null,
                lastAttemptAt,
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
        );
    }

    public OperationalNotificationDelivery deadLetter(
            String errorCode
    ) {
        return new OperationalNotificationDelivery(
                intent.withStatus(
                        NotificationDeliveryStatus.DEAD_LETTERED
                ),
                attemptCount,
                null,
                lastAttemptAt,
                deliveredAt,
                requiredErrorCode(errorCode),
                providerReference
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

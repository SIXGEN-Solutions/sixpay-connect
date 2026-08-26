package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NotificationAttempt(
        UUID attemptId,
        UUID notificationId,
        int attemptNumber,
        Instant startedAt,
        Instant completedAt,
        NotificationAttemptOutcome outcome,
        String errorCode
) {
    public NotificationAttempt {
        attemptId = Objects.requireNonNull(
                attemptId,
                "attemptId"
        );
        notificationId = Objects.requireNonNull(
                notificationId,
                "notificationId"
        );

        if (attemptNumber <= 0) {
            throw new IllegalArgumentException(
                    "attemptNumber must be positive"
            );
        }

        startedAt = Objects.requireNonNull(
                startedAt,
                "startedAt"
        );

        outcome = Objects.requireNonNull(
                outcome,
                "outcome"
        );

        errorCode = errorCode == null
                || errorCode.isBlank()
                ? null
                : errorCode.strip();

        if (outcome == NotificationAttemptOutcome.STARTED) {
            if (completedAt != null) {
                throw new IllegalArgumentException(
                        "STARTED attempt cannot be completed"
                );
            }
        } else if (completedAt == null) {
            throw new IllegalArgumentException(
                    "Completed attempt requires completedAt"
            );
        }
    }
}

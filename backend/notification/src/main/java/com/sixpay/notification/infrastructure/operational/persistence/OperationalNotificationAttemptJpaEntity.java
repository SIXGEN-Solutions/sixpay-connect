package com.sixpay.notification.infrastructure.operational.persistence;

import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationAttemptOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "operational_notification_attempts",
        schema = "sixpay",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_operational_notification_attempt_number",
                columnNames = {
                        "notification_id",
                        "attempt_number"
                }
        )
)
public class OperationalNotificationAttemptJpaEntity {

    @Id
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private NotificationAttemptOutcome outcome;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    protected OperationalNotificationAttemptJpaEntity() {
    }

    static OperationalNotificationAttemptJpaEntity from(
            NotificationAttempt attempt
    ) {
        var entity =
                new OperationalNotificationAttemptJpaEntity();
        entity.synchronize(attempt);
        return entity;
    }

    void synchronize(
            NotificationAttempt attempt
    ) {
        attemptId = attempt.attemptId();
        notificationId = attempt.notificationId();
        attemptNumber = attempt.attemptNumber();
        startedAt = attempt.startedAt();
        completedAt = attempt.completedAt();
        outcome = attempt.outcome();
        errorCode = attempt.errorCode();
    }

    UUID attemptId() {
        return attemptId;
    }

    UUID notificationId() {
        return notificationId;
    }

    int attemptNumber() {
        return attemptNumber;
    }

    Instant startedAt() {
        return startedAt;
    }

    Instant completedAt() {
        return completedAt;
    }

    NotificationAttemptOutcome outcome() {
        return outcome;
    }

    String errorCode() {
        return errorCode;
    }
}

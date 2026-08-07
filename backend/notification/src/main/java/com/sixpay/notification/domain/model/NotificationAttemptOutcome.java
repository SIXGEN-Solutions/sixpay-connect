package com.sixpay.notification.domain.model;

public enum NotificationAttemptOutcome {
    STARTED,
    ACCEPTED,
    DELIVERED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT
}

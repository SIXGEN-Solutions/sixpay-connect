package com.sixpay.notification.domain.model;

public enum NotificationDeliveryStatus {
    PENDING,
    DISPATCHING,
    ACCEPTED,
    DELIVERED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    DEAD_LETTERED
}

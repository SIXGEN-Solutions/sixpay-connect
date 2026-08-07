package com.sixpay.notification.domain.model;

public record NotificationDeduplicationKey(
        String value
) {
    public NotificationDeduplicationKey {
        if (value == null
                || !value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException(
                    "Notification deduplication key "
                            + "must be a SHA-256 hex value"
            );
        }
    }
}

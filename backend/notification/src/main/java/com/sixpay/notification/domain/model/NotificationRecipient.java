package com.sixpay.notification.domain.model;

import java.util.Locale;
import java.util.Objects;

public record NotificationRecipient(
        NotificationRecipientType type,
        String reference,
        Locale locale
) {
    public NotificationRecipient {
        type = Objects.requireNonNull(
                type,
                "type"
        );

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "reference is required"
            );
        }

        reference = reference.strip();

        locale = locale == null
                ? Locale.FRENCH
                : locale;
    }
}

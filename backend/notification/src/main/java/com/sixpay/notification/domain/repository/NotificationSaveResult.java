package com.sixpay.notification.domain.repository;

import com.sixpay.notification.domain.model.OperationalNotificationDelivery;

import java.util.Objects;

public record NotificationSaveResult(
        OperationalNotificationDelivery delivery,
        boolean created
) {
    public NotificationSaveResult {
        delivery = Objects.requireNonNull(
                delivery,
                "delivery"
        );
    }
}

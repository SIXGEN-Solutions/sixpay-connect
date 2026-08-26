package com.sixpay.notification.application.port.output;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;

import java.util.Objects;

public record NotificationDispatchResult(
        NotificationDeliveryStatus status,
        String providerReference
) {
    public NotificationDispatchResult {
        status = Objects.requireNonNull(
                status,
                "status"
        );

        if (status != NotificationDeliveryStatus.ACCEPTED
                && status != NotificationDeliveryStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "Dispatch result must be ACCEPTED or DELIVERED"
            );
        }

        providerReference =
                providerReference == null
                        || providerReference.isBlank()
                        ? null
                        : providerReference.strip();
    }

    public static NotificationDispatchResult accepted(
            String providerReference
    ) {
        return new NotificationDispatchResult(
                NotificationDeliveryStatus.ACCEPTED,
                providerReference
        );
    }

    public static NotificationDispatchResult delivered(
            String providerReference
    ) {
        return new NotificationDispatchResult(
                NotificationDeliveryStatus.DELIVERED,
                providerReference
        );
    }
}

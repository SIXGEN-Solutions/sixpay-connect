package com.sixpay.customer.observation.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Decoded keyset position for linked Payment observations.
 */
public record ObservedCustomerPaymentPosition(
        Instant lastPaymentCreatedAt,
        UUID lastPaymentId
) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerPaymentPosition {
        lastPaymentCreatedAt = Objects.requireNonNull(
                lastPaymentCreatedAt,
                "lastPaymentCreatedAt is required"
        );
        lastPaymentId = Objects.requireNonNull(
                lastPaymentId,
                "lastPaymentId is required"
        );

        if (NIL_UUID.equals(lastPaymentId)) {
            throw new IllegalArgumentException(
                    "lastPaymentId must not be nil"
            );
        }
    }
}

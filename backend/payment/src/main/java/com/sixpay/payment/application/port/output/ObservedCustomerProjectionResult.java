package com.sixpay.payment.application.port.output;

import java.util.Objects;
import java.util.UUID;

public record ObservedCustomerProjectionResult(
        UUID sourceEventId,
        Disposition disposition,
        long projectionVersion
) {

    public ObservedCustomerProjectionResult {
        sourceEventId = Objects.requireNonNull(
                sourceEventId,
                "sourceEventId is required"
        );
        disposition = Objects.requireNonNull(
                disposition,
                "disposition is required"
        );
        if (projectionVersion < 1) {
            throw new IllegalArgumentException(
                    "projectionVersion must be at least one"
            );
        }
    }

    public enum Disposition {
        APPLIED,
        REPLAYED,
        IGNORED_STALE
    }
}

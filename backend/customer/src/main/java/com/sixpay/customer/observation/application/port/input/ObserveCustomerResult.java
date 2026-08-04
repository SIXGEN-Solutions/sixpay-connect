package com.sixpay.customer.observation.application.port.input;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer-owned result of applying one observation to the projection.
 */
public record ObserveCustomerResult(
        ObservedCustomerId observedCustomerId,
        UUID sourceEventId,
        UUID paymentId,
        Disposition disposition,
        long projectionVersion,
        Instant processedAt
) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObserveCustomerResult {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
        sourceEventId = requireUuid(
                sourceEventId,
                "sourceEventId"
        );
        paymentId = requireUuid(
                paymentId,
                "paymentId"
        );
        disposition = Objects.requireNonNull(
                disposition,
                "disposition is required"
        );

        if (projectionVersion < 1) {
            throw new ObservedCustomerDomainException(
                    "projectionVersion must be at least one"
            );
        }

        processedAt = Objects.requireNonNull(
                processedAt,
                "processedAt is required"
        );
    }

    public boolean applied() {
        return disposition == Disposition.APPLIED;
    }

    public boolean replayed() {
        return disposition == Disposition.REPLAYED;
    }

    private static UUID requireUuid(
            UUID value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " is required"
        );

        if (NIL_UUID.equals(value)) {
            throw new ObservedCustomerDomainException(
                    fieldName + " must not be nil"
            );
        }

        return value;
    }

    public enum Disposition {
        APPLIED,
        REPLAYED,
        IGNORED_STALE
    }
}

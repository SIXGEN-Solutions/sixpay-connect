package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Stable query for Payment observations linked to one Observed Customer.
 */
public record ListObservedCustomerPaymentsQuery(
        ObservedCustomerId observedCustomerId,
        ObservedPaymentStatus status,
        Instant createdFrom,
        Instant createdTo,
        ObservedCustomerCursor cursor,
        int size,
        Instant snapshotAt
) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public ListObservedCustomerPaymentsQuery {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );

        if (createdFrom != null
                && createdTo != null
                && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException(
                    "createdFrom must not be after createdTo"
            );
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_SIZE
            );
        }

        snapshotAt = Objects.requireNonNull(
                snapshotAt,
                "snapshotAt is required"
        );
    }

    public static ListObservedCustomerPaymentsQuery firstPage(
            ObservedCustomerId observedCustomerId,
            ObservedPaymentStatus status,
            Instant createdFrom,
            Instant createdTo,
            Integer size,
            Instant snapshotAt
    ) {
        return new ListObservedCustomerPaymentsQuery(
                observedCustomerId,
                status,
                createdFrom,
                createdTo,
                null,
                size == null ? DEFAULT_SIZE : size,
                snapshotAt
        );
    }

    public boolean continuationPage() {
        return cursor != null;
    }
}

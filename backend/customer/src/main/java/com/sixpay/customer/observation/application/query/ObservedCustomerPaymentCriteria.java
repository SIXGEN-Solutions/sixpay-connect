package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Canonical Payment-list criteria after cursor authentication and decoding.
 */
public record ObservedCustomerPaymentCriteria(
        ObservedCustomerId observedCustomerId,
        ObservedPaymentStatus status,
        Instant createdFrom,
        Instant createdTo,
        int size,
        Instant snapshotAt,
        ObservedCustomerPaymentPosition position
) {

    public ObservedCustomerPaymentCriteria {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
        snapshotAt = Objects.requireNonNull(
                snapshotAt,
                "snapshotAt is required"
        );

        if (size < 1
                || size > ListObservedCustomerPaymentsQuery.MAX_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and "
                            + ListObservedCustomerPaymentsQuery.MAX_SIZE
            );
        }
    }
}

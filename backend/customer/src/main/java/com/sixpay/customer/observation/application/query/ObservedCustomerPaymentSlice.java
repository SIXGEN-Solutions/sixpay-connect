package com.sixpay.customer.observation.application.query;

import java.util.List;
import java.util.Objects;

/**
 * Repository slice of linked Payment observations.
 */
public record ObservedCustomerPaymentSlice(
        List<ObservedCustomerPaymentView> items,
        boolean hasMore,
        ObservedCustomerPaymentPosition nextPosition
) {

    public ObservedCustomerPaymentSlice {
        items = List.copyOf(
                Objects.requireNonNull(
                        items,
                        "items is required"
                )
        );

        if (hasMore && nextPosition == null) {
            throw new IllegalArgumentException(
                    "nextPosition is required when hasMore is true"
            );
        }

        if (!hasMore && nextPosition != null) {
            throw new IllegalArgumentException(
                    "nextPosition must be absent when hasMore is false"
            );
        }
    }
}

package com.sixpay.customer.observation.application.query;

import java.util.List;
import java.util.Objects;

/**
 * Repository slice containing at most the requested number of rows.
 */
public record ObservedCustomerSearchSlice(
        List<ObservedCustomerSummaryView> items,
        boolean hasMore,
        ObservedCustomerSearchPosition nextPosition
) {

    public ObservedCustomerSearchSlice {
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

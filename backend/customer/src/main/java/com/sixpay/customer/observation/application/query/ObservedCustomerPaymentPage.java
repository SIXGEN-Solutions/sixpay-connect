package com.sixpay.customer.observation.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stable cursor page of linked Payment observations.
 */
public record ObservedCustomerPaymentPage(
        List<ObservedCustomerPaymentView> items,
        int size,
        boolean hasMore,
        ObservedCustomerCursor nextCursor,
        Instant snapshotAt
) {

    public ObservedCustomerPaymentPage {
        items = List.copyOf(
                Objects.requireNonNull(
                        items,
                        "items is required"
                )
        );

        if (size != items.size()) {
            throw new IllegalArgumentException(
                    "size must equal the number of items"
            );
        }

        if (hasMore && nextCursor == null) {
            throw new IllegalArgumentException(
                    "nextCursor is required when hasMore is true"
            );
        }

        if (!hasMore && nextCursor != null) {
            throw new IllegalArgumentException(
                    "nextCursor must be absent when hasMore is false"
            );
        }

        snapshotAt = Objects.requireNonNull(
                snapshotAt,
                "snapshotAt is required"
        );
    }
}

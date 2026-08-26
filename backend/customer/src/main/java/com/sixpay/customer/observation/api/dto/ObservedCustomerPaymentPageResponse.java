package com.sixpay.customer.observation.api.dto;

import java.time.Instant;
import java.util.List;

public record ObservedCustomerPaymentPageResponse(
        List<ObservedCustomerPaymentResponse> items,
        int size,
        boolean hasMore,
        String nextCursor,
        Instant snapshotAt
) {
}

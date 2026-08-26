package com.sixpay.customer.observation.api.dto;

import java.time.Instant;
import java.util.List;

public record ObservedCustomerSearchPageResponse(
        List<ObservedCustomerSummaryResponse> items,
        int size,
        boolean hasMore,
        String nextCursor,
        Instant snapshotAt
) {
}

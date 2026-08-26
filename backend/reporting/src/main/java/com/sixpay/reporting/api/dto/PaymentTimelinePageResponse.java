package com.sixpay.reporting.api.dto;

import java.time.Instant;
import java.util.List;

public record PaymentTimelinePageResponse(
        List<PaymentTimelineEntryResponse> items,
        int size,
        boolean hasMore,
        String nextCursor,
        Instant snapshotAt
) {
}

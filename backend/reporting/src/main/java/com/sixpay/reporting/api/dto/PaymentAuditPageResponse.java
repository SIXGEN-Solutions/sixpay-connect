package com.sixpay.reporting.api.dto;

import java.time.Instant;
import java.util.List;

public record PaymentAuditPageResponse(
        List<PaymentAuditRecordResponse> items,
        int size,
        boolean hasMore,
        String nextCursor,
        Instant snapshotAt
) {
}

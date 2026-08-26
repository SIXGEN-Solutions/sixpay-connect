package com.sixpay.reporting.application.query;

import java.time.Instant;
import java.util.List;

public record PaymentAuditPage(
        List<PaymentAuditRecordView> items,
        int size,
        boolean hasMore,
        AuditCursor nextCursor,
        Instant snapshotAt
) {
    public PaymentAuditPage {
        items = List.copyOf(items);
    }
}

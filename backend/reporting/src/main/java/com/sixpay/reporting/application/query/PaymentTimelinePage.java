package com.sixpay.reporting.application.query;

import java.time.Instant;
import java.util.List;

public record PaymentTimelinePage(
        List<PaymentTimelineEntryView> items,
        int size,
        boolean hasMore,
        AuditCursor nextCursor,
        Instant snapshotAt
) {
    public PaymentTimelinePage {
        items = List.copyOf(items);
    }
}

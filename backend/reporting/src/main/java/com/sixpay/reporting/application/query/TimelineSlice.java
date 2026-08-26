package com.sixpay.reporting.application.query;

import java.util.List;

public record TimelineSlice(
        List<PaymentTimelineEntryView> items,
        boolean hasMore,
        AuditPosition nextPosition
) {
    public TimelineSlice {
        items = List.copyOf(items);
    }
}

package com.sixpay.reporting.application.query;

import java.util.List;

public record AuditSlice(
        List<PaymentAuditRecordView> items,
        boolean hasMore,
        AuditPosition nextPosition
) {
    public AuditSlice {
        items = List.copyOf(items);
    }
}

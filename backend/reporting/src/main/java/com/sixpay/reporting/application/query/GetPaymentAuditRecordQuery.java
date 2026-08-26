package com.sixpay.reporting.application.query;

import java.util.Objects;
import java.util.UUID;

public record GetPaymentAuditRecordQuery(UUID auditId) {
    public GetPaymentAuditRecordQuery {
        auditId = Objects.requireNonNull(auditId, "auditId is required");
    }
}

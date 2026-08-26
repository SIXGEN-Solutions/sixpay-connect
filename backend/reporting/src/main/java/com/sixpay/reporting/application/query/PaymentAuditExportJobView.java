package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditExportStatus;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record PaymentAuditExportJobView(
        UUID exportId,
        AuditExportStatus status,
        Instant requestedAt,
        String requestedBy,
        String businessPurpose,
        Long recordCount,
        String checksum,
        URI retrievalUri,
        Instant expiresAt,
        String failureCode
) {
}

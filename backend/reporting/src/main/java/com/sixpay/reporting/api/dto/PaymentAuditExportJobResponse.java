package com.sixpay.reporting.api.dto;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record PaymentAuditExportJobResponse(
        UUID exportId,
        String status,
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

package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditExportFormat;
import com.sixpay.reporting.domain.model.AuditExportStatus;
import com.sixpay.reporting.domain.model.AuditResult;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditExportJobDefinition(
        UUID exportId,
        String idempotencyKey,
        String requestFingerprint,
        AuditExportStatus status,
        Instant occurredFrom,
        Instant occurredTo,
        List<UUID> paymentIds,
        List<String> financialInstitutionCodes,
        List<String> actions,
        List<AuditResult> results,
        String businessPurpose,
        AuditExportFormat format,
        String requestedBy,
        UUID correlationId,
        Instant requestedAt,
        Instant expiresAt,
        Long recordCount,
        String checksum,
        URI retrievalUri,
        String failureCode
) {
    public PaymentAuditExportJobView toView() {
        return new PaymentAuditExportJobView(
                exportId,
                status,
                requestedAt,
                requestedBy,
                businessPurpose,
                recordCount,
                checksum,
                retrievalUri,
                expiresAt,
                failureCode
        );
    }
}

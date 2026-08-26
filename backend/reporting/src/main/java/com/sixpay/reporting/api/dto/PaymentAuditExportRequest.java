package com.sixpay.reporting.api.dto;

import com.sixpay.reporting.domain.model.AuditExportFormat;
import com.sixpay.reporting.domain.model.AuditResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentAuditExportRequest(
        @NotNull Instant occurredFrom,
        @NotNull Instant occurredTo,
        @Size(max = 1000) List<UUID> paymentIds,
        @Size(max = 100) List<
                @NotBlank @Size(max = 64) String
                > financialInstitutionCodes,
        @Size(max = 100) List<
                @NotBlank @Size(max = 100) String
                > actions,
        List<AuditResult> results,
        @NotBlank @Size(min = 10, max = 500)
        String businessPurpose,
        @NotNull AuditExportFormat format
) {
}

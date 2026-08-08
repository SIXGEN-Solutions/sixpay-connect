package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditExportFormat;
import com.sixpay.reporting.domain.model.AuditResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RequestPaymentAuditExportCommand(
        String idempotencyKey,
        Instant occurredFrom,
        Instant occurredTo,
        List<UUID> paymentIds,
        List<String> financialInstitutionCodes,
        List<String> actions,
        List<AuditResult> results,
        String businessPurpose,
        AuditExportFormat format,
        String requestedBy,
        UUID correlationId
) {
    public RequestPaymentAuditExportCommand {
        idempotencyKey = bounded(
                idempotencyKey, 1, 150, "idempotencyKey"
        );
        occurredFrom = Objects.requireNonNull(
                occurredFrom, "occurredFrom is required"
        );
        occurredTo = Objects.requireNonNull(
                occurredTo, "occurredTo is required"
        );
        if (occurredFrom.isAfter(occurredTo)) {
            throw new IllegalArgumentException(
                    "occurredFrom must not be after occurredTo"
            );
        }

        paymentIds = copy(paymentIds, 1000, "paymentIds");
        financialInstitutionCodes = copyText(
                financialInstitutionCodes, 100, 64,
                "financialInstitutionCodes"
        );
        actions = copyText(actions, 100, 100, "actions");
        results = copy(results, 16, "results");

        businessPurpose = bounded(
                businessPurpose, 10, 500, "businessPurpose"
        );
        format = Objects.requireNonNull(format, "format is required");
        requestedBy = bounded(
                requestedBy, 1, 128, "requestedBy"
        );
        correlationId = Objects.requireNonNull(
                correlationId, "correlationId is required"
        );
    }

    private static String bounded(
            String value,
            int min,
            int max,
            String field
    ) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() < min
                || normalized.length() > max
                || normalized.chars().anyMatch(
                        ch -> Character.isISOControl(ch)
                )) {
            throw new IllegalArgumentException(
                    field + " has invalid length/content"
            );
        }
        return normalized;
    }

    private static <T> List<T> copy(
            List<T> values,
            int max,
            String field
    ) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > max) {
            throw new IllegalArgumentException(
                    field + " exceeds maximum size " + max
            );
        }
        return List.copyOf(values);
    }

    private static List<String> copyText(
            List<String> values,
            int maxItems,
            int maxLength,
            String field
    ) {
        List<String> copied = copy(values, maxItems, field);
        return copied.stream()
                .map(value -> bounded(
                        value, 1, maxLength, field
                ))
                .distinct()
                .toList();
    }
}

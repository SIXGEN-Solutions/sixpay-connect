package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PartnerExternalPaymentStatusEvidence(
        String reference,
        String transactionId,
        String providerStatus,
        String paymentMethod,
        String operatorReference,
        boolean debitEffectue,
        boolean quittanceDisponible,
        Instant providerUpdatedAt,
        String failureReason,
        Instant checkedAt,
        String requestReference,
        String correlationId
) {
    public PartnerExternalPaymentStatusEvidence {
        reference = required(reference, "reference");
        transactionId = required(transactionId, "transactionId");
        providerStatus = required(providerStatus, "providerStatus");
        paymentMethod = required(paymentMethod, "paymentMethod");
        operatorReference = optional(operatorReference);
        providerUpdatedAt = Objects.requireNonNull(providerUpdatedAt, "providerUpdatedAt");
        failureReason = optional(failureReason);
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt");
        requestReference = required(requestReference, "requestReference");
        correlationId = required(correlationId, "correlationId");

        if (!reference.equals(requestReference)) {
            throw new IllegalArgumentException(
                    "Partner response reference must match requestReference"
            );
        }
    }

    public boolean confirmsPaidPayment() {
        return "COMPLETED".equalsIgnoreCase(providerStatus)
                || "NOT_REQUIRED".equalsIgnoreCase(providerStatus);
    }

    public static PartnerExternalPaymentStatusEvidence notRequired(
            String reference,
            Instant checkedAt,
            String correlationId
    ) {
        String normalized = required(reference, "reference");
        return new PartnerExternalPaymentStatusEvidence(
                normalized,
                "NOT_REQUIRED",
                "NOT_REQUIRED",
                "NOT_REQUIRED",
                null,
                false,
                false,
                checkedAt,
                null,
                checkedAt,
                normalized,
                required(correlationId, "correlationId")
        );
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

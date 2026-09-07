package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TresorPayPaymentStatusEvidence(
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
    public TresorPayPaymentStatusEvidence {
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
                    "TRESOR PAY response reference must match requestReference"
            );
        }
    }

    public boolean confirmsPaidPayment() {
        return "COMPLETED".equalsIgnoreCase(providerStatus);
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

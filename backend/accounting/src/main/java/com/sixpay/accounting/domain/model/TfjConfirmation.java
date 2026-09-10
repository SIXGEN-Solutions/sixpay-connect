package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record TfjConfirmation(
        UUID confirmationId,
        String idempotencyKey,
        String logicalPayloadHash,
        String financialInstitutionCode,
        LocalDate businessDate,
        String paymentReference,
        String bankPostingReference,
        String tfjBatchReference,
        TfjStatus status,
        Instant confirmedAt,
        String failureCode,
        String failureDescription,
        TfjRecoveryAction recoveryAction,
        Instant receivedAt,
        TfjObservationChannel observationChannel,
        String correlationId,
        TfjMatchStatus matchStatus,
        UUID matchedPaymentId,
        Instant finalityPublishedAt
) {
    public TfjConfirmation {
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        idempotencyKey = required(idempotencyKey, "idempotencyKey");
        logicalPayloadHash = required(logicalPayloadHash, "logicalPayloadHash");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        paymentReference = required(paymentReference, "paymentReference");
        bankPostingReference = required(bankPostingReference, "bankPostingReference");
        tfjBatchReference = optional(tfjBatchReference);
        status = Objects.requireNonNull(status, "status");
        confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
        failureCode = optional(failureCode);
        failureDescription = optional(failureDescription);
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        observationChannel = Objects.requireNonNull(observationChannel, "observationChannel");
        correlationId = required(correlationId, "correlationId");
        matchStatus = Objects.requireNonNull(matchStatus, "matchStatus");

        if (status == TfjStatus.FAILED) {
            if (failureCode == null || failureDescription == null || recoveryAction == null) {
                throw new IllegalArgumentException(
                        "FAILED TFJ confirmation requires failure code, description and recoveryAction"
                );
            }
        } else if (failureCode != null || failureDescription != null || recoveryAction != null) {
            throw new IllegalArgumentException(
                    "Only FAILED TFJ confirmation may contain failure information"
            );
        }

        if (matchStatus == TfjMatchStatus.MATCHED && matchedPaymentId == null) {
            throw new IllegalArgumentException("MATCHED TFJ confirmation requires matchedPaymentId");
        }
        if (matchStatus != TfjMatchStatus.MATCHED && matchedPaymentId != null) {
            throw new IllegalArgumentException("Only MATCHED TFJ confirmation may have matchedPaymentId");
        }
    }

    public boolean terminal() {
        return status == TfjStatus.INTEGRATED || status == TfjStatus.FAILED;
    }

    public TfjConfirmation withMatch(
            TfjMatchStatus newMatchStatus,
            UUID paymentId
    ) {
        return new TfjConfirmation(
                confirmationId, idempotencyKey, logicalPayloadHash,
                financialInstitutionCode, businessDate, paymentReference,
                bankPostingReference, tfjBatchReference, status, confirmedAt,
                failureCode, failureDescription, recoveryAction, receivedAt,
                observationChannel, correlationId, newMatchStatus, paymentId,
                finalityPublishedAt
        );
    }

    public TfjConfirmation withLogicalPayloadHash(String hash) {
        return new TfjConfirmation(
                confirmationId, idempotencyKey, hash,
                financialInstitutionCode, businessDate, paymentReference,
                bankPostingReference, tfjBatchReference, status, confirmedAt,
                failureCode, failureDescription, recoveryAction, receivedAt,
                observationChannel, correlationId, matchStatus, matchedPaymentId,
                finalityPublishedAt
        );
    }

    public TfjConfirmation markFinalityPublished(Instant at) {
        return new TfjConfirmation(
                confirmationId, idempotencyKey, logicalPayloadHash,
                financialInstitutionCode, businessDate, paymentReference,
                bankPostingReference, tfjBatchReference, status, confirmedAt,
                failureCode, failureDescription, recoveryAction, receivedAt,
                observationChannel, correlationId, matchStatus, matchedPaymentId,
                Objects.requireNonNull(at, "at")
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

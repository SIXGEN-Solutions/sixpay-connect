package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record TfjOperationalSnapshot(
        UUID confirmationId,
        String financialInstitutionCode,
        LocalDate businessDate,
        String paymentReference,
        String bankPostingReference,
        String tfjBatchReference,
        TfjStatus tfjStatus,
        Instant confirmedAt,
        TfjObservationChannel observationChannel,
        String correlationId,
        TfjMatchStatus matchStatus,
        UUID matchedPaymentId,
        Instant finalityPublishedAt,
        String failureCode,
        TfjRecoveryAction recoveryAction,
        TfjOperationalCategory category
) {
    public TfjOperationalSnapshot {
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        financialInstitutionCode = required(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        paymentReference = required(paymentReference, "paymentReference");
        bankPostingReference = required(
                bankPostingReference,
                "bankPostingReference"
        );
        tfjBatchReference = optional(tfjBatchReference);
        tfjStatus = Objects.requireNonNull(tfjStatus, "tfjStatus");
        confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
        observationChannel = Objects.requireNonNull(
                observationChannel,
                "observationChannel"
        );
        correlationId = required(correlationId, "correlationId");
        matchStatus = Objects.requireNonNull(matchStatus, "matchStatus");
        failureCode = optional(failureCode);
        category = Objects.requireNonNull(category, "category");

        if (matchStatus == TfjMatchStatus.MATCHED && matchedPaymentId == null) {
            throw new IllegalArgumentException(
                    "MATCHED TFJ snapshot requires matchedPaymentId"
            );
        }

        if (matchStatus != TfjMatchStatus.MATCHED && matchedPaymentId != null) {
            throw new IllegalArgumentException(
                    "Only MATCHED TFJ snapshot may expose matchedPaymentId"
            );
        }

        if (tfjStatus == TfjStatus.FAILED) {
            if (failureCode == null || recoveryAction == null) {
                throw new IllegalArgumentException(
                        "FAILED TFJ snapshot requires failureCode and recoveryAction"
                );
            }
        } else if (failureCode != null || recoveryAction != null) {
            throw new IllegalArgumentException(
                    "Only FAILED TFJ snapshot may expose failure information"
            );
        }
    }

    public static TfjOperationalSnapshot from(TfjConfirmation confirmation) {
        Objects.requireNonNull(confirmation, "confirmation");

        return new TfjOperationalSnapshot(
                confirmation.confirmationId(),
                confirmation.financialInstitutionCode(),
                confirmation.businessDate(),
                confirmation.paymentReference(),
                confirmation.bankPostingReference(),
                confirmation.tfjBatchReference(),
                confirmation.status(),
                confirmation.confirmedAt(),
                confirmation.observationChannel(),
                confirmation.correlationId(),
                confirmation.matchStatus(),
                confirmation.matchedPaymentId(),
                confirmation.finalityPublishedAt(),
                confirmation.failureCode(),
                confirmation.recoveryAction(),
                categoryOf(confirmation)
        );
    }

    private static TfjOperationalCategory categoryOf(
            TfjConfirmation confirmation
    ) {
        if (confirmation.matchStatus() == TfjMatchStatus.UNMATCHED) {
            return TfjOperationalCategory.QUARANTINED_UNMATCHED;
        }

        if (confirmation.matchStatus() == TfjMatchStatus.AMBIGUOUS) {
            return TfjOperationalCategory.QUARANTINED_AMBIGUOUS;
        }

        if (confirmation.status() == TfjStatus.FAILED) {
            return TfjOperationalCategory.FAILED;
        }

        if (confirmation.terminal()
                && confirmation.finalityPublishedAt() == null) {
            return TfjOperationalCategory.FINALITY_PUBLICATION_PENDING;
        }

        if (confirmation.status() == TfjStatus.INTEGRATED
                && confirmation.finalityPublishedAt() != null) {
            return TfjOperationalCategory.COMPLETED;
        }

        return TfjOperationalCategory.MATCHED;
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

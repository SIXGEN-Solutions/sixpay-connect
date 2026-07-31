package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted a uniquely matched final TFJ confirmation.
 */
public record PaymentEndOfDayConfirmationRecorded(
        PaymentEventMetadata metadata,
        TfjConfirmationId confirmationId,
        FinancialInstitutionCode financialInstitutionCode,
        LocalDate businessDate,
        String principalPostingReference,
        TfjStatus tfjStatus,
        FailureCode failureCode,
        TfjRecoveryAction recoveryAction,
        Instant confirmedAt,
        Instant matchedAt,
        EvidenceFingerprint evidenceFingerprint
) implements PaymentDomainEvent {

    public PaymentEndOfDayConfirmationRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        tfjStatus = Objects.requireNonNull(tfjStatus, "tfjStatus");
        confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
        matchedAt = Objects.requireNonNull(matchedAt, "matchedAt");
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        if (tfjStatus == TfjStatus.INTEGRATED
                && (failureCode != null || recoveryAction != null)) {
            throw new IllegalArgumentException(
                    "Integrated TFJ event must not contain failure data"
            );
        }
        if (tfjStatus == TfjStatus.FAILED
                && (failureCode == null || recoveryAction == null)) {
            throw new IllegalArgumentException(
                    "Failed TFJ event requires failure and recovery data"
            );
        }
    }
}

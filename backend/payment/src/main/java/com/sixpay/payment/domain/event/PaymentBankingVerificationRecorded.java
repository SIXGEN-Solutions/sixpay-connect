package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted a canonical banking verification result.
 */
public record PaymentBankingVerificationRecorded(
        PaymentEventMetadata metadata,
        BankingVerificationId verificationId,
        BankingVerificationOutcome outcome,
        List<SafeCheckResult> checkResults,
        EvidenceFingerprint evidenceFingerprint,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentBankingVerificationRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        verificationId = Objects.requireNonNull(verificationId, "verificationId");
        outcome = Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(checkResults, "checkResults");
        checkResults = List.copyOf(checkResults);
        if (checkResults.isEmpty()) {
            throw new IllegalArgumentException("checkResults must not be empty");
        }
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
    }
}

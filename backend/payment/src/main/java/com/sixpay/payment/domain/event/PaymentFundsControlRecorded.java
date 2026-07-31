package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted a canonical funds-control result for its exact amount and account.
 */
public record PaymentFundsControlRecorded(
        PaymentEventMetadata metadata,
        FundsVerificationReference verificationReference,
        FundsControlOutcome outcome,
        List<SafeCheckResult> checkResults,
        Instant validUntil,
        EvidenceFingerprint evidenceFingerprint,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentFundsControlRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        verificationReference = Objects.requireNonNull(verificationReference, "verificationReference");
        outcome = Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(checkResults, "checkResults");
        checkResults = List.copyOf(checkResults);
        if (checkResults.isEmpty()) {
            throw new IllegalArgumentException("checkResults must not be empty");
        }
        validUntil = Objects.requireNonNull(validUntil, "validUntil");
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
    }
}

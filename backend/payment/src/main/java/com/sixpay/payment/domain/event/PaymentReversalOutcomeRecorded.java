package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted the current direct canonical reversal outcome.
 */
public record PaymentReversalOutcomeRecorded(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        ReversalOutcome outcome,
        ReversalReference reversalReference,
        String reversalEntryReference,
        FailureCode reasonCode,
        EvidenceFingerprint evidenceFingerprint,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentReversalOutcomeRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (reversalEntryReference != null) {
            reversalEntryReference = reversalEntryReference.strip();
            if (reversalEntryReference.isEmpty() || reversalEntryReference.length() > 256) {
                throw new IllegalArgumentException("reversalEntryReference has an invalid length");
            }
        }
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
        if (outcome == ReversalOutcome.REVERSED
                && reversalReference == null) {
            throw new IllegalArgumentException(
                    "Reversed outcome requires a reversal reference"
            );
        }
        if ((outcome == ReversalOutcome.REJECTED
                || outcome == ReversalOutcome.NOT_ALLOWED)
                && reasonCode == null) {
            throw new IllegalArgumentException(
                    "Rejected reversal outcome requires a reason code"
            );
        }
    }
}

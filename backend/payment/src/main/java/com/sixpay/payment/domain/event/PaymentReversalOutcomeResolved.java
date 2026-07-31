package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * An authoritative lookup has resolved the original uncertain reversal.
 */
public record PaymentReversalOutcomeResolved(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        ReversalOutcome previousOutcome,
        ReversalOutcome resolvedOutcome,
        ReversalReference reversalReference,
        String reversalEntryReference,
        FailureCode reasonCode,
        EvidenceFingerprint evidenceFingerprint,
        Instant resolvedAt
) implements PaymentDomainEvent {

    public PaymentReversalOutcomeResolved {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        previousOutcome = Objects.requireNonNull(previousOutcome, "previousOutcome");
        resolvedOutcome = Objects.requireNonNull(resolvedOutcome, "resolvedOutcome");
        if (reversalEntryReference != null) {
            reversalEntryReference = reversalEntryReference.strip();
            if (reversalEntryReference.isEmpty() || reversalEntryReference.length() > 256) {
                throw new IllegalArgumentException("reversalEntryReference has an invalid length");
            }
        }
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt");
        if (previousOutcome != ReversalOutcome.UNKNOWN
                || resolvedOutcome == ReversalOutcome.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Reversal resolution must replace UNKNOWN with a conclusive outcome"
            );
        }
        if (resolvedOutcome == ReversalOutcome.REVERSED
                && reversalReference == null) {
            throw new IllegalArgumentException(
                    "Reversed outcome requires a reversal reference"
            );
        }
        if ((resolvedOutcome == ReversalOutcome.REJECTED
                || resolvedOutcome == ReversalOutcome.NOT_ALLOWED)
                && reasonCode == null) {
            throw new IllegalArgumentException(
                    "Rejected reversal outcome requires a reason code"
            );
        }
    }
}

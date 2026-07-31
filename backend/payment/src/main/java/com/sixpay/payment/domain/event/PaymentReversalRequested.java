package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The explicitly authorized reversal has been durably requested.
 */
public record PaymentReversalRequested(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        ReversalIdempotencyKey reversalIdempotencyKey,
        String originalPostingReference,
        EvidenceFingerprint instructionFingerprint,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentReversalRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        reversalIdempotencyKey = Objects.requireNonNull(reversalIdempotencyKey, "reversalIdempotencyKey");
        originalPostingReference = Objects.requireNonNull(originalPostingReference, "originalPostingReference");
        originalPostingReference = originalPostingReference.strip();
        if (originalPostingReference.isEmpty() || originalPostingReference.length() > 256) {
            throw new IllegalArgumentException("originalPostingReference has an invalid length");
        }
        instructionFingerprint = Objects.requireNonNull(instructionFingerprint, "instructionFingerprint");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

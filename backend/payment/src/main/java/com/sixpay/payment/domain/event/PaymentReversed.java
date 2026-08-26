package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The explicitly authorized reversal is confirmed while original posting evidence remains.
 */
public record PaymentReversed(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        String originalPostingReference,
        ReversalReference reversalReference,
        Instant reversedAt
) implements PaymentDomainEvent {

    public PaymentReversed {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        originalPostingReference = Objects.requireNonNull(originalPostingReference, "originalPostingReference");
        originalPostingReference = originalPostingReference.strip();
        if (originalPostingReference.isEmpty() || originalPostingReference.length() > 256) {
            throw new IllegalArgumentException("originalPostingReference has an invalid length");
        }
        reversalReference = Objects.requireNonNull(reversalReference, "reversalReference");
        reversedAt = Objects.requireNonNull(reversedAt, "reversedAt");
    }
}

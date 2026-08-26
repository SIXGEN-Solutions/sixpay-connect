package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested resolution of an uncertain reversal outcome.
 */
public record PaymentReversalOutcomeLookupRequested(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        ReversalIdempotencyKey reversalIdempotencyKey,
        ReversalReference reversalReference,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentReversalOutcomeLookupRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        reversalIdempotencyKey = Objects.requireNonNull(reversalIdempotencyKey, "reversalIdempotencyKey");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * One reversal instruction has been explicitly and durably authorized.
 */
public record PaymentReversalAuthorized(
        PaymentEventMetadata metadata,
        ReversalInstructionId reversalInstructionId,
        ReversalIdempotencyKey reversalIdempotencyKey,
        String originalPostingReference,
        ReversalAuthorizationType authorizationType,
        ReversalAuthorizationReference authorizationReference,
        FailureCode reasonCode,
        Instant authorizedAt
) implements PaymentDomainEvent {

    public PaymentReversalAuthorized {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        reversalInstructionId = Objects.requireNonNull(reversalInstructionId, "reversalInstructionId");
        reversalIdempotencyKey = Objects.requireNonNull(reversalIdempotencyKey, "reversalIdempotencyKey");
        originalPostingReference = Objects.requireNonNull(originalPostingReference, "originalPostingReference");
        originalPostingReference = originalPostingReference.strip();
        if (originalPostingReference.isEmpty() || originalPostingReference.length() > 256) {
            throw new IllegalArgumentException("originalPostingReference has an invalid length");
        }
        authorizationType = Objects.requireNonNull(authorizationType, "authorizationType");
        authorizationReference = Objects.requireNonNull(authorizationReference, "authorizationReference");
        reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
        authorizedAt = Objects.requireNonNull(authorizedAt, "authorizedAt");
    }
}

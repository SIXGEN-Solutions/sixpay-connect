package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested authoritative resolution of an uncertain posting.
 */
public record PaymentPostingOutcomeLookupRequested(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingIdempotencyKey postingIdempotencyKey,
        String principalPostingReference,
        PostingLookupMode lookupMode,
        Instant unknownSince,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentPostingOutcomeLookupRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        postingIdempotencyKey = Objects.requireNonNull(postingIdempotencyKey, "postingIdempotencyKey");
        if (principalPostingReference != null) {
            principalPostingReference = principalPostingReference.strip();
            if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
                throw new IllegalArgumentException("principalPostingReference has an invalid length");
            }
        }
        lookupMode = Objects.requireNonNull(lookupMode, "lookupMode");
        unknownSince = Objects.requireNonNull(unknownSince, "unknownSince");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

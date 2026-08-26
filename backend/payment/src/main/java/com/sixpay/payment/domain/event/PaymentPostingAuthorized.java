package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The sole logical posting instruction has been durably authorized.
 */
public record PaymentPostingAuthorized(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingIdempotencyKey postingIdempotencyKey,
        EvidenceFingerprint postingInstructionFingerprint,
        Instant authorizedAt
) implements PaymentDomainEvent {

    public PaymentPostingAuthorized {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        postingIdempotencyKey = Objects.requireNonNull(postingIdempotencyKey, "postingIdempotencyKey");
        postingInstructionFingerprint = Objects.requireNonNull(postingInstructionFingerprint, "postingInstructionFingerprint");
        authorizedAt = Objects.requireNonNull(authorizedAt, "authorizedAt");
    }
}

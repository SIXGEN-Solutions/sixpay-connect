package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * An authoritative lookup has resolved the original uncertain posting.
 */
public record PaymentPostingOutcomeResolved(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingOutcome previousOutcome,
        PostingOutcome resolvedOutcome,
        String principalPostingReference,
        PostingLegPayload debitLeg,
        PostingLegPayload cutCreditLeg,
        LocalDate businessDate,
        FailureCode rejectionCode,
        EvidenceFingerprint evidenceFingerprint,
        Instant resolvedAt
) implements PaymentDomainEvent {

    public PaymentPostingOutcomeResolved {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        previousOutcome = Objects.requireNonNull(previousOutcome, "previousOutcome");
        resolvedOutcome = Objects.requireNonNull(resolvedOutcome, "resolvedOutcome");
        if (principalPostingReference != null) {
            principalPostingReference = principalPostingReference.strip();
            if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
                throw new IllegalArgumentException("principalPostingReference has an invalid length");
            }
        }
        debitLeg = Objects.requireNonNull(debitLeg, "debitLeg");
        cutCreditLeg = Objects.requireNonNull(cutCreditLeg, "cutCreditLeg");
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt");
        if (previousOutcome != PostingOutcome.UNKNOWN
                || resolvedOutcome == PostingOutcome.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Posting resolution must replace UNKNOWN with a conclusive outcome"
            );
        }
    }
}

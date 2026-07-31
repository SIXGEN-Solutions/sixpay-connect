package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted the current direct canonical posting outcome.
 */
public record PaymentPostingOutcomeRecorded(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingOutcome outcome,
        String principalPostingReference,
        PostingLegPayload debitLeg,
        PostingLegPayload cutCreditLeg,
        LocalDate businessDate,
        FailureCode rejectionCode,
        PostingNextAction nextAction,
        EvidenceFingerprint evidenceFingerprint,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentPostingOutcomeRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (principalPostingReference != null) {
            principalPostingReference = principalPostingReference.strip();
            if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
                throw new IllegalArgumentException("principalPostingReference has an invalid length");
            }
        }
        debitLeg = Objects.requireNonNull(debitLeg, "debitLeg");
        cutCreditLeg = Objects.requireNonNull(cutCreditLeg, "cutCreditLeg");
        nextAction = Objects.requireNonNull(nextAction, "nextAction");
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
    }
}

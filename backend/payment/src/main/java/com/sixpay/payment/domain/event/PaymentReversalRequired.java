package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment requires explicit reversal review because a financial effect exists or is authoritatively reconciled.
 */
public record PaymentReversalRequired(
        PaymentEventMetadata metadata,
        String principalPostingReference,
        FailureCode reasonCode,
        ReversalSourceStage sourceStage,
        PostingLegStatus knownDebitStatus,
        PostingLegStatus knownCutCreditStatus,
        Instant requiredAt
) implements PaymentDomainEvent {

    public PaymentReversalRequired {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
        sourceStage = Objects.requireNonNull(sourceStage, "sourceStage");
        knownDebitStatus = Objects.requireNonNull(knownDebitStatus, "knownDebitStatus");
        knownCutCreditStatus = Objects.requireNonNull(knownCutCreditStatus, "knownCutCreditStatus");
        requiredAt = Objects.requireNonNull(requiredAt, "requiredAt");
    }
}

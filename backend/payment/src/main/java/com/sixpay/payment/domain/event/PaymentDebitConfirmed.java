package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The debtor debit is confirmed while complete CUT credit is not yet confirmed.
 */
public record PaymentDebitConfirmed(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        String principalPostingReference,
        String debitLegReference,
        LocalDate businessDate,
        Instant debitedAt
) implements PaymentDomainEvent {

    public PaymentDebitConfirmed {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        if (debitLegReference != null) {
            debitLegReference = debitLegReference.strip();
            if (debitLegReference.isEmpty() || debitLegReference.length() > 256) {
                throw new IllegalArgumentException("debitLegReference has an invalid length");
            }
        }
    }
}

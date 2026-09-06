package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.PaymentEventObservationSource;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Payment accepted one atomic Core Banking T0 event outcome.
 */
public record PaymentEventOutcomeRecorded(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingIdempotencyKey postingIdempotencyKey,
        PaymentEventOutcome outcome,
        String principalBankReference,
        FailureCode reasonCode,
        PaymentEventObservationSource observationSource,
        LocalDate accountingDate,
        Instant observedAt
) implements PaymentDomainEvent {

    public PaymentEventOutcomeRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(
                postingInstructionId,
                "Posting instruction ID"
        );
        postingIdempotencyKey = Objects.requireNonNull(
                postingIdempotencyKey,
                "Posting idempotency key"
        );
        outcome = Objects.requireNonNull(outcome, "Payment event outcome");
        observationSource = Objects.requireNonNull(
                observationSource,
                "Observation source"
        );
        accountingDate = Objects.requireNonNull(
                accountingDate,
                "Accounting date"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "Observed instant"
        );
        if (principalBankReference != null) {
            principalBankReference = principalBankReference.strip();
            if (principalBankReference.isEmpty()
                    || principalBankReference.length() > 128) {
                throw new IllegalArgumentException(
                        "principalBankReference has an invalid length"
                );
            }
        }
    }
}

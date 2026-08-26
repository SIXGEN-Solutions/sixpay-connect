package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The sole authorized posting has been durably requested.
 */
public record PaymentPostingRequested(
        PaymentEventMetadata metadata,
        PostingInstructionId postingInstructionId,
        PostingIdempotencyKey postingIdempotencyKey,
        EvidenceFingerprint postingInstructionFingerprint,
        FinancialInstitutionCode financialInstitutionCode,
        MoneyPayload requestedAmount,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentPostingRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        postingInstructionId = Objects.requireNonNull(postingInstructionId, "postingInstructionId");
        postingIdempotencyKey = Objects.requireNonNull(postingIdempotencyKey, "postingIdempotencyKey");
        postingInstructionFingerprint = Objects.requireNonNull(postingInstructionFingerprint, "postingInstructionFingerprint");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

/**
 * Immutable identity and safe fingerprint of the sole logical posting.
 */
public record PostingInstructionIdentity(
        PostingInstructionId instructionId,
        PostingIdempotencyKey idempotencyKey,
        Money amount,
        String accountBindingFingerprint,
        EvidenceFingerprint instructionFingerprint
) {
    public PostingInstructionIdentity {
        instructionId = Objects.requireNonNull(
                instructionId,
                "Posting instruction ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Posting idempotency key"
        );
        amount = Objects.requireNonNull(amount, "Posting amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Posting amount must be positive"
            );
        }
        Objects.requireNonNull(
                accountBindingFingerprint,
                "Account binding fingerprint"
        );
        if (!accountBindingFingerprint.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Account binding fingerprint has an invalid format"
            );
        }
        instructionFingerprint = Objects.requireNonNull(
                instructionFingerprint,
                "Posting instruction fingerprint"
        );
    }
}

package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.ReversalIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.ReversalInstructionId;

import java.util.Objects;

public record ReversalInstructionIdentity(
        ReversalInstructionId instructionId,
        ReversalIdempotencyKey idempotencyKey
) {
    public ReversalInstructionIdentity {
        instructionId = Objects.requireNonNull(
                instructionId,
                "Reversal instruction ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Reversal idempotency key"
        );
    }
}

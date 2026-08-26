package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.ReversalIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.ReversalInstructionId;

import java.util.Objects;

/**
 * Immutable identity and safe fingerprint of one authorized reversal.
 */
public record ReversalInstructionIdentity(
        ReversalInstructionId instructionId,
        ReversalIdempotencyKey idempotencyKey,
        EvidenceFingerprint instructionFingerprint
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
        instructionFingerprint = Objects.requireNonNull(
                instructionFingerprint,
                "Reversal instruction fingerprint"
        );
    }
}

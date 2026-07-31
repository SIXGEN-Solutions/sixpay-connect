package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

public final class ReversalSnapshot implements ValueObject {

    private final BankPostingReference originalBankPostingReference;
    private final ReversalInstructionId reversalInstructionId;
    private final ReversalIdempotencyKey reversalCommandIdempotencyKey;
    private final ReversalAuthorizationEvidence authorization;
    private final ReversalOutcomeEvidence outcome;

    public ReversalSnapshot(
            BankPostingReference originalBankPostingReference,
            ReversalInstructionId reversalInstructionId,
            ReversalIdempotencyKey reversalCommandIdempotencyKey,
            ReversalAuthorizationEvidence authorization,
            ReversalOutcomeEvidence outcome
    ) {
        this.originalBankPostingReference =
                EvidenceValueObjectRules.requireNonNull(
                        originalBankPostingReference,
                        "Original bank posting reference"
                );
        this.reversalInstructionId =
                EvidenceValueObjectRules.requireNonNull(
                        reversalInstructionId,
                        "Reversal instruction ID"
                );
        this.reversalCommandIdempotencyKey =
                EvidenceValueObjectRules.requireNonNull(
                        reversalCommandIdempotencyKey,
                        "Reversal idempotency key"
                );
        this.authorization = EvidenceValueObjectRules.requireNonNull(
                authorization,
                "Reversal authorization evidence"
        );
        this.outcome = outcome;
    }

    public BankPostingReference originalBankPostingReference() {
        return originalBankPostingReference;
    }

    public ReversalInstructionId reversalInstructionId() {
        return reversalInstructionId;
    }

    public ReversalIdempotencyKey reversalCommandIdempotencyKey() {
        return reversalCommandIdempotencyKey;
    }

    public ReversalAuthorizationEvidence authorization() {
        return authorization;
    }

    public Optional<ReversalOutcomeEvidence> outcome() {
        return Optional.ofNullable(outcome);
    }

    public ReversalSnapshot withOutcome(
            ReversalOutcomeEvidence newOutcome
    ) {
        return new ReversalSnapshot(
                originalBankPostingReference,
                reversalInstructionId,
                reversalCommandIdempotencyKey,
                authorization,
                EvidenceValueObjectRules.requireNonNull(
                        newOutcome,
                        "Reversal outcome evidence"
                )
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReversalSnapshot that)) {
            return false;
        }
        return originalBankPostingReference.equals(
                that.originalBankPostingReference
        ) && reversalInstructionId.equals(that.reversalInstructionId)
                && reversalCommandIdempotencyKey.equals(
                        that.reversalCommandIdempotencyKey
                )
                && authorization.equals(that.authorization)
                && Objects.equals(outcome, that.outcome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                originalBankPostingReference,
                reversalInstructionId,
                reversalCommandIdempotencyKey,
                authorization,
                outcome
        );
    }

    @Override
    public String toString() {
        return "ReversalSnapshot[originalPostingReference="
                + originalBankPostingReference
                + ", instructionId=" + reversalInstructionId
                + ", outcome="
                + (outcome == null ? "PENDING" : outcome.outcome())
                + "]";
    }
}

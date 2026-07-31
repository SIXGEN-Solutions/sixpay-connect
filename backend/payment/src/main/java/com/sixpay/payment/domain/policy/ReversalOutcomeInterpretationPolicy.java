package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.ReversalOutcome;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;

import java.util.Objects;

public final class ReversalOutcomeInterpretationPolicy {

    public ReversalOutcomeInterpretation decide(
            PaymentReversalContext context,
            ReversalSnapshot evidence,
            PaymentFailure failure,
            FinancialOutcomePolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Reversal context");
        Objects.requireNonNull(evidence, "Reversal evidence");
        Objects.requireNonNull(profile, "Financial outcome profile");

        if (!context.instructionId().equals(
                evidence.reversalInstructionId()
        ) || !context.idempotencyKey().equals(
                evidence.reversalCommandIdempotencyKey()
        )) {
            return new ReversalOutcomeInterpretation(
                    ReversalDecision.CONFLICT,
                    failure
            );
        }

        if (evidence.outcome().isEmpty()) {
            return new ReversalOutcomeInterpretation(
                    ReversalDecision.REVERSAL_REQUIRED,
                    failure
            );
        }

        ReversalOutcome outcome = evidence.outcome()
                .orElseThrow()
                .outcome();

        ReversalDecision decision = switch (outcome) {
            case REVERSED -> ReversalDecision.REVERSED;
            case UNKNOWN ->
                    ReversalDecision.REVERSAL_OUTCOME_UNKNOWN;
            case REJECTED, NOT_ALLOWED ->
                    ReversalDecision.REVERSAL_REQUIRED;
        };

        return new ReversalOutcomeInterpretation(decision, failure);
    }
}

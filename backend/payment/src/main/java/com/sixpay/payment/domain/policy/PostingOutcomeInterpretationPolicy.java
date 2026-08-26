package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.PostingOutcome;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.util.Objects;

public final class PostingOutcomeInterpretationPolicy {

    public PostingOutcomeInterpretation decide(
            PaymentPostingContext context,
            PostingOutcomeSnapshot evidence,
            PaymentFailure failure,
            FinancialOutcomePolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Posting context");
        Objects.requireNonNull(evidence, "Posting evidence");
        Objects.requireNonNull(profile, "Financial outcome profile");

        if (!context.instructionId().equals(
                evidence.postingInstructionId()
        ) || !context.idempotencyKey().equals(
                evidence.postingCommandIdempotencyKey()
        ) || !context.amount().equals(evidence.amount())) {
            return new PostingOutcomeInterpretation(
                    PostingDecision.CONFLICT,
                    failure
            );
        }

        PostingDecision decision = switch (evidence.outcome()) {
            case COMPLETED ->
                    PostingDecision.POSTED_PENDING_TFJ;
            case DEBIT_CONFIRMED_CUT_CREDIT_PENDING ->
                    PostingDecision.DEBIT_CONFIRMED;
            case UNKNOWN ->
                    PostingDecision.POSTING_OUTCOME_UNKNOWN;
            case REVERSAL_REQUIRED ->
                    PostingDecision.REVERSAL_REQUIRED;
            case REJECTED_NO_FINANCIAL_EFFECT ->
                    failure == null
                            ? PostingDecision.REJECTED_NO_EFFECT
                            : PostingDecision.FAILED_NO_EFFECT;
        };

        return new PostingOutcomeInterpretation(
                decision,
                failure
        );
    }
}

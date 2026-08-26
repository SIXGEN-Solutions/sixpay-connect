package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.domain.policy.*;

import java.time.Instant;
import java.util.Objects;

public record PostingDecisionInput(
        PaymentPostingContext context,
        PostingOutcomeSnapshot candidate,
        PaymentFailure failure,
        CurrentPostingEvidence currentEvidence,
        EvidenceAuthority candidateAuthority,
        EvidenceConclusiveness candidateConclusiveness,
        PaymentLifecycleContext lifecycleContext,
        Instant decisionAt
) {
    public PostingDecisionInput {
        context = Objects.requireNonNull(context, "Posting context");
        candidate = Objects.requireNonNull(candidate, "Posting candidate");
        candidateAuthority = Objects.requireNonNull(
                candidateAuthority,
                "Candidate authority"
        );
        candidateConclusiveness = Objects.requireNonNull(
                candidateConclusiveness,
                "Candidate conclusiveness"
        );
        lifecycleContext = Objects.requireNonNull(
                lifecycleContext,
                "Lifecycle context"
        );
        decisionAt = Objects.requireNonNull(
                decisionAt,
                "Decision instant"
        );
    }
}

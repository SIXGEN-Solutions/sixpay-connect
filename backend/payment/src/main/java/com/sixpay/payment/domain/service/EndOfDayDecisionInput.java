package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.policy.*;

import java.time.Instant;
import java.util.Objects;

public record EndOfDayDecisionInput(
        PaymentTfjContext context,
        EndOfDayConfirmationSnapshot candidate,
        UniqueTfjMatchProof matchProof,
        PaymentFailure failure,
        CurrentTfjEvidence currentEvidence,
        EvidenceAuthority candidateAuthority,
        EvidenceConclusiveness candidateConclusiveness,
        PaymentLifecycleContext lifecycleContext,
        Instant decisionAt
) {
    public EndOfDayDecisionInput {
        context = Objects.requireNonNull(context, "TFJ context");
        candidate = Objects.requireNonNull(candidate, "TFJ candidate");
        matchProof = Objects.requireNonNull(matchProof, "TFJ match proof");
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

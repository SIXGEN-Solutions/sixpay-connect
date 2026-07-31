package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationEvidence;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import com.sixpay.payment.domain.policy.*;

import java.time.Instant;
import java.util.Objects;

public record ReversalDecisionInput(
        PaymentReversalEligibilityContext eligibilityContext,
        PaymentReversalContext outcomeContext,
        ReversalInstructionIdentity candidateInstruction,
        ReversalAuthorizationEvidence authorization,
        ReversalSnapshot candidateSnapshot,
        PaymentFailure failure,
        CurrentReversalEvidence currentEvidence,
        EvidenceAuthority candidateAuthority,
        EvidenceConclusiveness candidateConclusiveness,
        PaymentLifecycleContext lifecycleContext,
        Instant decisionAt
) {
    public ReversalDecisionInput {
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

    public boolean isAuthorizationRequest() {
        return candidateSnapshot == null;
    }
}

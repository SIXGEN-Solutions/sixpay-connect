package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.policy.*;

import java.util.Objects;

public final class PostingOutcomeDecisionService {

    private final EvidenceReplayReplacementPolicy replayPolicy;
    private final PostingOutcomeInterpretationPolicy interpretationPolicy;
    private final FailureClassificationPolicy failurePolicy;

    public PostingOutcomeDecisionService() {
        this(
                new EvidenceReplayReplacementPolicy(),
                new PostingOutcomeInterpretationPolicy(),
                new FailureClassificationPolicy()
        );
    }

    public PostingOutcomeDecisionService(
            EvidenceReplayReplacementPolicy replayPolicy,
            PostingOutcomeInterpretationPolicy interpretationPolicy,
            FailureClassificationPolicy failurePolicy
    ) {
        this.replayPolicy = Objects.requireNonNull(
                replayPolicy,
                "Replay policy"
        );
        this.interpretationPolicy = Objects.requireNonNull(
                interpretationPolicy,
                "Posting interpretation policy"
        );
        this.failurePolicy = Objects.requireNonNull(
                failurePolicy,
                "Failure classification policy"
        );
    }

    public PolicyDecision<PostingDecision> decide(
            PostingDecisionInput input,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(input, "Posting decision input");
        Objects.requireNonNull(profiles, "Payment policy bundle");

        EvidenceIdentity candidateIdentity = new EvidenceIdentity(
                input.candidate().postingInstructionId()
                        + ":"
                        + input.candidate().metadata()
                                .observationChannel()
                        + ":"
                        + input.candidate().metadata().acceptedAt(),
                input.candidate().metadata().evidenceFingerprint()
        );

        CurrentPostingEvidence current = input.currentEvidence();
        PolicyDecision<EvidenceReplayDecision> replay =
                replayPolicy.decide(
                        current == null ? null : current.identity(),
                        candidateIdentity,
                        current == null ? null : current.authority(),
                        input.candidateAuthority(),
                        current == null
                                ? null
                                : current.conclusiveness(),
                        input.candidateConclusiveness(),
                        input.lifecycleContext()
                );

        if (replay.decision()
                == EvidenceReplayDecision.NO_OP_IDENTICAL) {
            return PolicyDecision.of(
                    PostingDecision.NO_OP,
                    replay.reasonCode()
            );
        }

        if (replay.decision()
                == EvidenceReplayDecision.QUARANTINE_CONFLICT
                || replay.decision()
                == EvidenceReplayDecision.REJECT_CONFLICT
                || replay.decision()
                == EvidenceReplayDecision
                        .REJECT_TERMINAL_REPLACEMENT) {
            return PolicyDecision.of(
                    PostingDecision.CONFLICT,
                    replay.reasonCode()
            );
        }

        if (input.candidate().outcome()
                == com.sixpay.payment.domain.model.evidence
                        .PostingOutcome
                        .REJECTED_NO_FINANCIAL_EFFECT) {
            if (input.failure() == null) {
                return PolicyDecision.of(
                        PostingDecision.CONFLICT,
                        "REJECTED_POSTING_REQUIRES_CLASSIFIED_FAILURE"
                );
            }

            PolicyDecision<FailureDispositionDecision> disposition =
                    failurePolicy.decide(
                            input.failure(),
                            FinancialEffectKnowledge.PROVEN_NONE,
                            input.context().status(),
                            profiles.failureClassificationProfile()
                    );

            PostingDecision classified = switch (
                    disposition.decision()
            ) {
                case BUSINESS_REJECT, SECURITY_REJECT ->
                        PostingDecision.REJECTED_NO_EFFECT;
                case TECHNICAL_FAIL_NO_EFFECT ->
                        PostingDecision.FAILED_NO_EFFECT;
                case DEFER, OUTCOME_UNKNOWN, REVERSAL_REQUIRED ->
                        PostingDecision.CONFLICT;
            };

            return PolicyDecision.withProfile(
                    classified,
                    disposition.reasonCode(),
                    profiles.failureClassificationProfile().metadata()
            );
        }

        PostingOutcomeInterpretation interpretation =
                interpretationPolicy.decide(
                        input.context(),
                        input.candidate(),
                        input.failure(),
                        profiles.financialOutcomePolicyProfile()
                );

        return PolicyDecision.withProfile(
                interpretation.decision(),
                "POSTING_OUTCOME_INTERPRETED",
                profiles.financialOutcomePolicyProfile().metadata()
        );
    }
}

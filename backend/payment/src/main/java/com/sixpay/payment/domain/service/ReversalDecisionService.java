package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.policy.*;

import java.util.Objects;

public final class ReversalDecisionService {

    private final EvidenceReplayReplacementPolicy replayPolicy;
    private final ReversalAuthorizationPolicy authorizationPolicy;
    private final ReversalOutcomeInterpretationPolicy outcomePolicy;

    public ReversalDecisionService() {
        this(
                new EvidenceReplayReplacementPolicy(),
                new ReversalAuthorizationPolicy(),
                new ReversalOutcomeInterpretationPolicy()
        );
    }

    public ReversalDecisionService(
            EvidenceReplayReplacementPolicy replayPolicy,
            ReversalAuthorizationPolicy authorizationPolicy,
            ReversalOutcomeInterpretationPolicy outcomePolicy
    ) {
        this.replayPolicy = Objects.requireNonNull(
                replayPolicy,
                "Replay policy"
        );
        this.authorizationPolicy = Objects.requireNonNull(
                authorizationPolicy,
                "Reversal authorization policy"
        );
        this.outcomePolicy = Objects.requireNonNull(
                outcomePolicy,
                "Reversal outcome policy"
        );
    }

    public PolicyDecision<ReversalDecision> decide(
            ReversalDecisionInput input,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(input, "Reversal decision input");
        Objects.requireNonNull(profiles, "Payment policy bundle");

        if (input.isAuthorizationRequest()) {
            PolicyDecision<ReversalAuthorizationDecision> authorization =
                    authorizationPolicy.decide(
                            input.eligibilityContext(),
                            input.candidateInstruction(),
                            input.authorization(),
                            input.decisionAt(),
                            profiles.reversalPolicyProfile()
                    );

            ReversalDecision mapped = switch (
                    authorization.decision()
            ) {
                case AUTHORIZE -> ReversalDecision.AUTHORIZE;
                case NO_OP_SAME_INSTRUCTION ->
                        ReversalDecision.NO_OP;
                case REJECT_INELIGIBLE,
                        REJECT_SECOND_OR_CONFLICTING_INSTRUCTION ->
                        ReversalDecision.CONFLICT;
            };

            return PolicyDecision.withProfile(
                    mapped,
                    authorization.reasonCode(),
                    profiles.reversalPolicyProfile().metadata()
            );
        }

        EvidenceIdentity candidateIdentity = new EvidenceIdentity(
                input.candidateSnapshot().reversalInstructionId()
                        + ":"
                        + input.candidateSnapshot()
                                .outcome()
                                .orElseThrow()
                                .metadata()
                                .observationChannel()
                        + ":"
                        + input.candidateSnapshot()
                                .outcome()
                                .orElseThrow()
                                .metadata()
                                .acceptedAt(),
                input.candidateSnapshot()
                        .outcome()
                        .orElseThrow()
                        .metadata()
                        .evidenceFingerprint()
        );

        CurrentReversalEvidence current = input.currentEvidence();
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
                    ReversalDecision.NO_OP,
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
                    ReversalDecision.CONFLICT,
                    replay.reasonCode()
            );
        }

        ReversalOutcomeInterpretation interpretation =
                outcomePolicy.decide(
                        input.outcomeContext(),
                        input.candidateSnapshot(),
                        input.failure(),
                        profiles.financialOutcomePolicyProfile()
                );

        return PolicyDecision.withProfile(
                interpretation.decision(),
                "REVERSAL_OUTCOME_INTERPRETED",
                profiles.financialOutcomePolicyProfile().metadata()
        );
    }
}

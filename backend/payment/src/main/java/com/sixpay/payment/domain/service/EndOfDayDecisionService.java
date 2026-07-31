package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.policy.*;

import java.util.Objects;

public final class EndOfDayDecisionService {

    private final EvidenceReplayReplacementPolicy replayPolicy;
    private final EndOfDayConfirmationAcceptancePolicy acceptancePolicy;

    public EndOfDayDecisionService() {
        this(
                new EvidenceReplayReplacementPolicy(),
                new EndOfDayConfirmationAcceptancePolicy()
        );
    }

    public EndOfDayDecisionService(
            EvidenceReplayReplacementPolicy replayPolicy,
            EndOfDayConfirmationAcceptancePolicy acceptancePolicy
    ) {
        this.replayPolicy = Objects.requireNonNull(
                replayPolicy,
                "Replay policy"
        );
        this.acceptancePolicy = Objects.requireNonNull(
                acceptancePolicy,
                "End-of-day acceptance policy"
        );
    }

    public PolicyDecision<EndOfDayDecision> decide(
            EndOfDayDecisionInput input,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(input, "End-of-day decision input");
        Objects.requireNonNull(profiles, "Payment policy bundle");

        EvidenceIdentity candidateIdentity = new EvidenceIdentity(
                input.candidate().confirmationId().toString(),
                input.candidate().metadata().evidenceFingerprint()
        );

        CurrentTfjEvidence current = input.currentEvidence();
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
                    EndOfDayDecision.NO_OP,
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
                    EndOfDayDecision.QUARANTINE_CONFLICT,
                    replay.reasonCode()
            );
        }

        EndOfDayInterpretation interpretation =
                acceptancePolicy.decide(
                        input.context(),
                        input.candidate(),
                        input.matchProof(),
                        input.decisionAt(),
                        profiles.tfjPolicyProfile(),
                        input.failure()
                );

        return PolicyDecision.withProfile(
                interpretation.decision(),
                "TFJ_OUTCOME_INTERPRETED",
                profiles.tfjPolicyProfile().metadata()
        );
    }
}

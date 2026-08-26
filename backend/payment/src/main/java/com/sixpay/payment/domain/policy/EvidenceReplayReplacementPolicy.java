package com.sixpay.payment.domain.policy;

import java.util.Objects;

public final class EvidenceReplayReplacementPolicy {

    public PolicyDecision<EvidenceReplayDecision> decide(
            EvidenceIdentity current,
            EvidenceIdentity candidate,
            EvidenceAuthority currentAuthority,
            EvidenceAuthority candidateAuthority,
            EvidenceConclusiveness currentConclusiveness,
            EvidenceConclusiveness candidateConclusiveness,
            PaymentLifecycleContext lifecycle
    ) {
        Objects.requireNonNull(candidate, "Candidate evidence identity");
        Objects.requireNonNull(candidateAuthority, "Candidate authority");
        Objects.requireNonNull(
                candidateConclusiveness,
                "Candidate conclusiveness"
        );
        Objects.requireNonNull(lifecycle, "Lifecycle context");

        if (current == null) {
            return PolicyDecision.of(
                    EvidenceReplayDecision.ACCEPT_NEW,
                    "NO_CURRENT_EVIDENCE"
            );
        }

        Objects.requireNonNull(currentAuthority, "Current authority");
        Objects.requireNonNull(
                currentConclusiveness,
                "Current conclusiveness"
        );

        if (current.identity().equals(candidate.identity())
                && current.fingerprint().equals(candidate.fingerprint())) {
            return PolicyDecision.of(
                    EvidenceReplayDecision.NO_OP_IDENTICAL,
                    "IDENTICAL_EVIDENCE_REPLAY"
            );
        }

        if (lifecycle.terminal()) {
            return PolicyDecision.of(
                    EvidenceReplayDecision.REJECT_TERMINAL_REPLACEMENT,
                    "TERMINAL_EVIDENCE_IMMUTABLE"
            );
        }

        if (current.identity().equals(candidate.identity())
                && !current.fingerprint().equals(candidate.fingerprint())) {
            return PolicyDecision.of(
                    EvidenceReplayDecision.QUARANTINE_CONFLICT,
                    "SAME_IDENTITY_DIFFERENT_FINGERPRINT"
            );
        }

        if (candidateAuthority.rank() > currentAuthority.rank()
                || candidateConclusiveness.rank()
                > currentConclusiveness.rank()) {
            return PolicyDecision.of(
                    EvidenceReplayDecision.REPLACE_MORE_AUTHORITATIVE,
                    "MORE_AUTHORITATIVE_OR_CONCLUSIVE_EVIDENCE"
            );
        }

        return PolicyDecision.of(
                EvidenceReplayDecision.REJECT_CONFLICT,
                "CONFLICTING_EVIDENCE_NOT_MORE_AUTHORITATIVE"
        );
    }
}

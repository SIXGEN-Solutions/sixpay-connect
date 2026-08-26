package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class EvidenceTemporalValidityPolicy {

    public PolicyDecision<EvidenceTemporalDecision> decide(
            EvidenceMetadata metadata,
            EvidenceCategory category,
            Instant decisionAt,
            EvidenceTemporalProfile profile
    ) {
        Objects.requireNonNull(metadata, "Evidence metadata");
        Objects.requireNonNull(category, "Evidence category");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Temporal profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)) {
            return PolicyDecision.withProfile(
                    EvidenceTemporalDecision.INVALID_CHRONOLOGY,
                    "PROFILE_NOT_EFFECTIVE",
                    profile.metadata()
            );
        }

        if (metadata.acceptedAt().isBefore(metadata.observedAt())) {
            return PolicyDecision.withProfile(
                    EvidenceTemporalDecision.INVALID_CHRONOLOGY,
                    "ACCEPTED_BEFORE_OBSERVED",
                    profile.metadata()
            );
        }

        if (metadata.observedAt().isAfter(
                decisionAt.plus(profile.maximumFutureSkew())
        )) {
            return PolicyDecision.withProfile(
                    EvidenceTemporalDecision.FUTURE_DATED,
                    "OBSERVATION_AFTER_ALLOWED_FUTURE_SKEW",
                    profile.metadata()
            );
        }

        Duration age = Duration.between(
                metadata.observedAt(),
                decisionAt
        );

        if (age.compareTo(profile.maximumAge(category)) > 0) {
            return PolicyDecision.withProfile(
                    EvidenceTemporalDecision.STALE,
                    "EVIDENCE_STALE",
                    profile.metadata()
            );
        }

        return PolicyDecision.withProfile(
                EvidenceTemporalDecision.VALID,
                "EVIDENCE_TEMPORALLY_VALID",
                profile.metadata()
        );
    }
}

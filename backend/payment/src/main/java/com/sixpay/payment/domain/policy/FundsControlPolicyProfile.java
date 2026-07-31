package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;

import java.util.Objects;
import java.util.Set;

public record FundsControlPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<FundsControlCheckType> mandatoryChecks,
        EvidenceTemporalProfile temporalProfile
) {
    public FundsControlPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(mandatoryChecks, "Mandatory checks");
        if (mandatoryChecks.isEmpty()
                || mandatoryChecks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Mandatory funds checks must not be empty"
            );
        }
        mandatoryChecks = Set.copyOf(mandatoryChecks);
        temporalProfile = Objects.requireNonNull(
                temporalProfile,
                "Temporal profile"
        );
    }
}

package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;

import java.util.Objects;
import java.util.Set;

public record BankingVerificationPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<BankingVerificationCheckType> mandatoryChecks,
        EvidenceTemporalProfile temporalProfile
) {
    public BankingVerificationPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(mandatoryChecks, "Mandatory checks");
        if (mandatoryChecks.isEmpty()
                || mandatoryChecks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Mandatory banking checks must not be empty"
            );
        }
        mandatoryChecks = Set.copyOf(mandatoryChecks);
        temporalProfile = Objects.requireNonNull(
                temporalProfile,
                "Temporal profile"
        );
    }
}

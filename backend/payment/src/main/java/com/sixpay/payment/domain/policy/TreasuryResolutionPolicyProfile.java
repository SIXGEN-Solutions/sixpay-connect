package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record TreasuryResolutionPolicyProfile(
        PolicyProfileMetadata metadata,
        Map<FinancialInstitutionCode, Set<String>>
                approvedConfigurationVersionsByInstitution
) {
    public TreasuryResolutionPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(
                approvedConfigurationVersionsByInstitution,
                "Approved configuration versions"
        );
        if (approvedConfigurationVersionsByInstitution.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one institution configuration must be supplied"
            );
        }
        approvedConfigurationVersionsByInstitution =
                approvedConfigurationVersionsByInstitution.entrySet()
                        .stream()
                        .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                Map.Entry::getKey,
                                entry -> Set.copyOf(entry.getValue())
                        ));
    }

    public boolean isApproved(
            FinancialInstitutionCode institution,
            String version
    ) {
        return approvedConfigurationVersionsByInstitution
                .getOrDefault(institution, Set.of())
                .contains(version);
    }
}

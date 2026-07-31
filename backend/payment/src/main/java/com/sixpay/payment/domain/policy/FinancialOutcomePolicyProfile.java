package com.sixpay.payment.domain.policy;

import java.util.Map;
import java.util.Objects;

public record FinancialOutcomePolicyProfile(
        PolicyProfileMetadata metadata,
        Map<EvidenceAuthority, Integer> authorityRanks,
        Map<EvidenceConclusiveness, Integer> conclusivenessRanks
) {
    public FinancialOutcomePolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        authorityRanks = immutableCompleteAuthorityMap(authorityRanks);
        conclusivenessRanks =
                immutableCompleteConclusivenessMap(conclusivenessRanks);
    }

    public int authorityRank(EvidenceAuthority authority) {
        return authorityRanks.get(authority);
    }

    public int conclusivenessRank(EvidenceConclusiveness value) {
        return conclusivenessRanks.get(value);
    }

    private static Map<EvidenceAuthority, Integer>
            immutableCompleteAuthorityMap(
                    Map<EvidenceAuthority, Integer> values
            ) {
        Objects.requireNonNull(values, "Authority ranks");
        if (!values.keySet().containsAll(
                java.util.Set.of(EvidenceAuthority.values())
        )) {
            throw new IllegalArgumentException(
                    "Authority ranks must cover every EvidenceAuthority"
            );
        }
        return Map.copyOf(values);
    }

    private static Map<EvidenceConclusiveness, Integer>
            immutableCompleteConclusivenessMap(
                    Map<EvidenceConclusiveness, Integer> values
            ) {
        Objects.requireNonNull(values, "Conclusiveness ranks");
        if (!values.keySet().containsAll(
                java.util.Set.of(EvidenceConclusiveness.values())
        )) {
            throw new IllegalArgumentException(
                    "Conclusiveness ranks must cover every EvidenceConclusiveness"
            );
        }
        return Map.copyOf(values);
    }
}

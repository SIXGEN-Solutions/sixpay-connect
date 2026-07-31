package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Objects;
import java.util.Set;

public record PostingAuthorizationPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<PaymentStatus> eligibleStatuses,
        boolean requireFreshFundsEvidence,
        boolean requireResolvedTreasuryAccount
) {
    public PostingAuthorizationPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(eligibleStatuses, "Eligible statuses");
        if (eligibleStatuses.isEmpty()) {
            throw new IllegalArgumentException(
                    "Eligible posting statuses must not be empty"
            );
        }
        eligibleStatuses = Set.copyOf(eligibleStatuses);
    }
}

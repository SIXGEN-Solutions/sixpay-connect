package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.TfjRecoveryAction;
import com.sixpay.payment.domain.model.evidence.TfjStatus;

import java.util.Objects;
import java.util.Set;

public record TfjPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<TfjStatus> acceptedFinalStatuses,
        Set<TfjRecoveryAction> reversalRequiredActions
) {
    public TfjPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(
                acceptedFinalStatuses,
                "Accepted final statuses"
        );
        Objects.requireNonNull(
                reversalRequiredActions,
                "Reversal-required actions"
        );
        acceptedFinalStatuses = Set.copyOf(acceptedFinalStatuses);
        reversalRequiredActions = Set.copyOf(reversalRequiredActions);
    }
}

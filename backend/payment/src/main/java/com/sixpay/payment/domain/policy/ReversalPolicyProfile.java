package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationType;

import java.util.Objects;
import java.util.Set;

public record ReversalPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<ReversalAuthorizationType> acceptedAuthorizationTypes,
        Set<String> acceptedReasonCodes
) {
    public ReversalPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(
                acceptedAuthorizationTypes,
                "Accepted authorization types"
        );
        Objects.requireNonNull(
                acceptedReasonCodes,
                "Accepted reversal reason codes"
        );
        if (acceptedAuthorizationTypes.isEmpty()
                || acceptedReasonCodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Reversal authorization profile must not be empty"
            );
        }
        acceptedAuthorizationTypes =
                Set.copyOf(acceptedAuthorizationTypes);
        acceptedReasonCodes = Set.copyOf(acceptedReasonCodes);
    }
}

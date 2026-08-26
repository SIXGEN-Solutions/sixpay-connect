package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.AuthorizationBindingType;

import java.util.Objects;
import java.util.Set;

public record AuthorizationPolicyProfile(
        PolicyProfileMetadata metadata,
        Set<String> allowedIssuers,
        Set<String> allowedAlgorithms,
        Set<String> allowedScopes,
        Set<AuthorizationBindingType> mandatoryBindings
) {
    public AuthorizationPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        allowedIssuers = immutableNonEmpty(allowedIssuers, "Allowed issuers");
        allowedAlgorithms = immutableNonEmpty(
                allowedAlgorithms,
                "Allowed algorithms"
        );
        allowedScopes = immutableNonEmpty(allowedScopes, "Allowed scopes");
        mandatoryBindings = immutableNonEmpty(
                mandatoryBindings,
                "Mandatory bindings"
        );
    }

    private static <T> Set<T> immutableNonEmpty(
            Set<T> values,
            String label
    ) {
        Objects.requireNonNull(values, label);
        if (values.isEmpty() || values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        return Set.copyOf(values);
    }
}

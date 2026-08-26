package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.RetryDisposition;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record FailureClassificationProfile(
        PolicyProfileMetadata metadata,
        Map<FailureCategory, Set<RetryDisposition>> allowedDispositions
) {
    public FailureClassificationProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(
                allowedDispositions,
                "Allowed failure dispositions"
        );
        allowedDispositions = allowedDispositions.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> Set.copyOf(entry.getValue())
                ));
    }

    public boolean allows(
            FailureCategory category,
            RetryDisposition disposition
    ) {
        return allowedDispositions
                .getOrDefault(category, Set.of())
                .contains(disposition);
    }
}

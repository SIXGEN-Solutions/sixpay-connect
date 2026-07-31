package com.sixpay.payment.domain.policy;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record EvidenceTemporalProfile(
        PolicyProfileMetadata metadata,
        Duration maximumFutureSkew,
        Map<EvidenceCategory, Duration> maximumAgeByCategory
) {
    public EvidenceTemporalProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        maximumFutureSkew = requireNonNegative(
                maximumFutureSkew,
                "Maximum future skew"
        );
        Objects.requireNonNull(
                maximumAgeByCategory,
                "Maximum-age map"
        );
        maximumAgeByCategory = Map.copyOf(maximumAgeByCategory);
        maximumAgeByCategory.forEach((category, duration) -> {
            Objects.requireNonNull(category, "Evidence category");
            requireNonNegative(duration, "Maximum evidence age");
        });
    }

    public Duration maximumAge(EvidenceCategory category) {
        Duration value = maximumAgeByCategory.get(category);
        if (value == null) {
            throw new IllegalArgumentException(
                    "No maximum age configured for " + category
            );
        }
        return value;
    }

    private static Duration requireNonNegative(
            Duration value,
            String label
    ) {
        Objects.requireNonNull(value, label);
        if (value.isNegative()) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return value;
    }
}

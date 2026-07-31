package com.sixpay.payment.domain.policy;

import java.util.Objects;
import java.util.Optional;

public record PolicyDecision<T extends Enum<T>>(
        T decision,
        String reasonCode,
        String profileId,
        String profileVersion
) {
    public PolicyDecision {
        decision = Objects.requireNonNull(decision, "Decision");
        reasonCode = requireText(reasonCode, "Reason code");
        profileId = normalizeOptional(profileId);
        profileVersion = normalizeOptional(profileVersion);
    }

    public static <T extends Enum<T>> PolicyDecision<T> of(
            T decision,
            String reasonCode
    ) {
        return new PolicyDecision<>(
                decision,
                reasonCode,
                null,
                null
        );
    }

    public static <T extends Enum<T>> PolicyDecision<T> withProfile(
            T decision,
            String reasonCode,
            PolicyProfileMetadata metadata
    ) {
        Objects.requireNonNull(metadata, "Policy profile metadata");
        return new PolicyDecision<>(
                decision,
                reasonCode,
                metadata.profileId(),
                metadata.profileVersion()
        );
    }

    public Optional<String> profileIdOptional() {
        return Optional.ofNullable(profileId);
    }

    public Optional<String> profileVersionOptional() {
        return Optional.ofNullable(profileVersion);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        String canonical = value.strip();
        if (canonical.isEmpty() || canonical.length() > 128) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to 128 characters"
            );
        }
        return canonical;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String canonical = value.strip();
        if (canonical.isEmpty() || canonical.length() > 128) {
            throw new IllegalArgumentException(
                    "Optional profile metadata must contain 1 to 128 characters"
            );
        }
        return canonical;
    }
}

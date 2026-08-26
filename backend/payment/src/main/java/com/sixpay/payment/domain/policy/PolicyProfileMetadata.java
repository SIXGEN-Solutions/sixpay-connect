package com.sixpay.payment.domain.policy;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record PolicyProfileMetadata(
        String profileId,
        String profileVersion,
        Instant effectiveFrom,
        Instant effectiveUntil,
        String approvedByReference
) implements ValueObject {

    public PolicyProfileMetadata {
        profileId = requireText(profileId, "Profile ID");
        profileVersion = requireText(profileVersion, "Profile version");
        effectiveFrom = Objects.requireNonNull(
                effectiveFrom,
                "Effective-from instant must not be null"
        );
        approvedByReference = requireText(
                approvedByReference,
                "Approved-by reference"
        );

        if (effectiveUntil != null
                && effectiveUntil.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException(
                    "Profile effective-until must not precede effective-from"
            );
        }
    }

    public boolean isEffectiveAt(Instant instant) {
        Objects.requireNonNull(instant, "Decision instant must not be null");
        return !instant.isBefore(effectiveFrom)
                && (effectiveUntil == null
                || !instant.isAfter(effectiveUntil));
    }

    public Optional<Instant> effectiveUntilOptional() {
        return Optional.ofNullable(effectiveUntil);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String canonical = value.strip();
        if (canonical.isEmpty() || canonical.length() > 128) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to 128 characters"
            );
        }
        return canonical;
    }
}

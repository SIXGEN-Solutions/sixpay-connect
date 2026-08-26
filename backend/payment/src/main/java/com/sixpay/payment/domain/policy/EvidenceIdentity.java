package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;

import java.util.Objects;

public record EvidenceIdentity(
        String identity,
        EvidenceFingerprint fingerprint
) {
    public EvidenceIdentity {
        Objects.requireNonNull(identity, "Evidence identity");
        identity = identity.strip();
        if (identity.isEmpty() || identity.length() > 256) {
            throw new IllegalArgumentException(
                    "Evidence identity must contain 1 to 256 characters"
            );
        }
        fingerprint = Objects.requireNonNull(
                fingerprint,
                "Evidence fingerprint"
        );
    }
}

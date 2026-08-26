package com.sixpay.customer.verification.application.port.output;

import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationEvidence;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.policy.RequiredVerificationChecksPolicy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Customer-native response returned by the banking verification port.
 */
public record BankingVerificationResponse(
        List<VerificationCheck> checks,
        VerificationEvidenceFingerprint evidenceFingerprint,
        Instant observedAt,
        Instant validUntil
) {

    public BankingVerificationResponse {
        checks = RequiredVerificationChecksPolicy.requireComplete(checks);
        evidenceFingerprint = Objects.requireNonNull(
                evidenceFingerprint,
                "evidenceFingerprint is required"
        );
        observedAt = Objects.requireNonNull(observedAt, "observedAt is required");

        if (validUntil != null && validUntil.isBefore(observedAt)) {
            throw new IllegalArgumentException(
                    "validUntil must not be before observedAt"
            );
        }
    }

    public static BankingVerificationResponse of(
            Collection<VerificationCheck> checks,
            VerificationEvidenceFingerprint evidenceFingerprint,
            Instant observedAt,
            Instant validUntil
    ) {
        return new BankingVerificationResponse(
                List.copyOf(Objects.requireNonNull(checks, "checks are required")),
                evidenceFingerprint,
                observedAt,
                validUntil
        );
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }

    public VerificationEvidence toEvidence() {
        return VerificationEvidence.of(
                checks,
                evidenceFingerprint,
                observedAt,
                validUntil
        );
    }
}

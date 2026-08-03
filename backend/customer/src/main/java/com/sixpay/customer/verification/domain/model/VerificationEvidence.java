package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.policy.RequiredVerificationChecksPolicy;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable canonical banking evidence accepted by Customer Verification.
 *
 * @param checks complete and canonical mandatory check set
 * @param fingerprint immutable evidence fingerprint
 * @param observedAt instant at which the banking evidence was observed
 * @param validUntil optional latest instant at which the evidence remains fresh
 */
public record VerificationEvidence(
        List<VerificationCheck> checks,
        VerificationEvidenceFingerprint fingerprint,
        Instant observedAt,
        Instant validUntil
) implements ValueObject {

    public VerificationEvidence {
        checks = RequiredVerificationChecksPolicy.requireComplete(checks);
        fingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint is required"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );

        if (validUntil != null && validUntil.isBefore(observedAt)) {
            throw new CustomerVerificationDomainException(
                    "validUntil must not be before observedAt"
            );
        }
    }

    public static VerificationEvidence of(
            Collection<VerificationCheck> checks,
            VerificationEvidenceFingerprint fingerprint,
            Instant observedAt,
            Instant validUntil
    ) {
        return new VerificationEvidence(
                List.copyOf(
                        Objects.requireNonNull(
                                checks,
                                "checks are required"
                        )
                ),
                fingerprint,
                observedAt,
                validUntil
        );
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }
}

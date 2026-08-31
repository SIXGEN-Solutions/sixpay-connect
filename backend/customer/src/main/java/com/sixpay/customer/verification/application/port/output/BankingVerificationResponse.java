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
        Instant validUntil,
        String customerReference,
        String accountReference,
        VerifiedBankingIdentity identity,
        VerifiedBankingAccount account
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

        if (customerReference != null) {
            customerReference = customerReference.strip();
            if (customerReference.isBlank()) {
                throw new IllegalArgumentException(
                        "customerReference must not be blank"
                );
            }
        }

        if (accountReference != null) {
            accountReference = accountReference.strip();
            if (accountReference.isBlank()) {
                throw new IllegalArgumentException(
                        "accountReference must not be blank"
                );
            }
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
                validUntil,
                null,
                null,
                null,
                null
        );
    }

    public static BankingVerificationResponse of(
            Collection<VerificationCheck> checks,
            VerificationEvidenceFingerprint evidenceFingerprint,
            Instant observedAt,
            Instant validUntil,
            String customerReference,
            String accountReference,
            VerifiedBankingIdentity identity,
            VerifiedBankingAccount account
    ) {
        return new BankingVerificationResponse(
                List.copyOf(Objects.requireNonNull(checks, "checks are required")),
                evidenceFingerprint,
                observedAt,
                validUntil,
                customerReference,
                accountReference,
                identity,
                account
        );
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }

    public Optional<String> customerReferenceOptional() {
        return Optional.ofNullable(customerReference);
    }

    public Optional<String> accountReferenceOptional() {
        return Optional.ofNullable(accountReference);
    }

    public Optional<VerifiedBankingIdentity> identityOptional() {
        return Optional.ofNullable(identity);
    }

    public Optional<VerifiedBankingAccount> accountOptional() {
        return Optional.ofNullable(account);
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

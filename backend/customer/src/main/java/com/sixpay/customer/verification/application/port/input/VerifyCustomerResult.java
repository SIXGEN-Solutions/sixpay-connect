package com.sixpay.customer.verification.application.port.input;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationResult;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import com.sixpay.customer.verification.domain.policy.RequiredVerificationChecksPolicy;
import com.sixpay.customer.verification.domain.policy.VerificationOutcomePolicy;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VerifyCustomerResult(
        CustomerVerificationId verificationId,
        VerificationOutcome outcome,
        List<VerificationCheck> checks,
        VerificationEvidenceFingerprint evidenceFingerprint,
        AccountBindingFingerprint accountBindingFingerprint,
        Instant observedAt,
        Instant validUntil,
        Instant completedAt
) {
    public VerifyCustomerResult {
        verificationId = Objects.requireNonNull(verificationId, "verificationId is required");
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        checks = RequiredVerificationChecksPolicy.requireComplete(checks);
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint is required");
        accountBindingFingerprint = Objects.requireNonNull(accountBindingFingerprint, "accountBindingFingerprint is required");
        observedAt = Objects.requireNonNull(observedAt, "observedAt is required");
        completedAt = Objects.requireNonNull(completedAt, "completedAt is required");

        VerificationOutcome derived = VerificationOutcomePolicy.determine(checks);
        if (outcome != derived) {
            throw new CustomerVerificationDomainException(
                    "VerifyCustomerResult outcome is inconsistent with checks"
            );
        }
        if (validUntil != null && validUntil.isBefore(observedAt)) {
            throw new CustomerVerificationDomainException(
                    "validUntil must not be before observedAt"
            );
        }
        if (completedAt.isBefore(observedAt)) {
            throw new CustomerVerificationDomainException(
                    "completedAt must not be before observedAt"
            );
        }
    }

    public static VerifyCustomerResult from(
            CustomerVerificationId verificationId,
            CustomerVerificationResult result,
            AccountBindingFingerprint accountBindingFingerprint
    ) {
        Objects.requireNonNull(result, "result is required");
        return new VerifyCustomerResult(
                verificationId,
                result.outcome(),
                result.evidence().checks(),
                result.evidence().fingerprint(),
                accountBindingFingerprint,
                result.evidence().observedAt(),
                result.evidence().validUntil(),
                result.completedAt()
        );
    }

    public static VerifyCustomerResult of(
            CustomerVerificationId verificationId,
            VerificationOutcome outcome,
            Collection<VerificationCheck> checks,
            VerificationEvidenceFingerprint evidenceFingerprint,
            AccountBindingFingerprint accountBindingFingerprint,
            Instant observedAt,
            Instant validUntil,
            Instant completedAt
    ) {
        return new VerifyCustomerResult(
                verificationId,
                outcome,
                List.copyOf(Objects.requireNonNull(checks, "checks are required")),
                evidenceFingerprint,
                accountBindingFingerprint,
                observedAt,
                validUntil,
                completedAt
        );
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }

    @Override
    public String toString() {
        return "VerifyCustomerResult[verificationId=" + verificationId
                + ", outcome=" + outcome
                + ", checks=" + checks
                + ", evidenceFingerprint=[PROTECTED]"
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", observedAt=" + observedAt
                + ", validUntil=" + validUntil
                + ", completedAt=" + completedAt + "]";
    }
}

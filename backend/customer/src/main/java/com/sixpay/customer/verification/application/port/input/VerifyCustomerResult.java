package com.sixpay.customer.verification.application.port.input;

import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;
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
        Instant completedAt,
        String customerReference,
        String accountReference,
        VerifiedBankingIdentity identity,
        VerifiedBankingAccount account
) {
    public VerifyCustomerResult {
        verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        checks = RequiredVerificationChecksPolicy.requireComplete(checks);
        evidenceFingerprint = Objects.requireNonNull(
                evidenceFingerprint,
                "evidenceFingerprint is required"
        );
        accountBindingFingerprint = Objects.requireNonNull(
                accountBindingFingerprint,
                "accountBindingFingerprint is required"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );
        completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt is required"
        );

        VerificationOutcome derived =
                VerificationOutcomePolicy.determine(checks);
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

        if (outcome == VerificationOutcome.VERIFIED) {
            requireVerifiedBankingContext(
                    customerReference,
                    accountReference,
                    identity,
                    account
            );
        }
    }

    public static VerifyCustomerResult from(
            CustomerVerificationId verificationId,
            CustomerVerificationResult result,
            AccountBindingFingerprint accountBindingFingerprint,
            BankingVerificationResponse bankingResponse
    ) {
        Objects.requireNonNull(result, "result is required");
        Objects.requireNonNull(
                bankingResponse,
                "bankingResponse is required"
        );

        return new VerifyCustomerResult(
                verificationId,
                result.outcome(),
                result.evidence().checks(),
                result.evidence().fingerprint(),
                accountBindingFingerprint,
                result.evidence().observedAt(),
                result.evidence().validUntil(),
                result.completedAt(),
                bankingResponse.customerReference(),
                bankingResponse.accountReference(),
                bankingResponse.identity(),
                bankingResponse.account()
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
            Instant completedAt,
            String customerReference,
            String accountReference,
            VerifiedBankingIdentity identity,
            VerifiedBankingAccount account
    ) {
        return new VerifyCustomerResult(
                verificationId,
                outcome,
                List.copyOf(
                        Objects.requireNonNull(checks, "checks are required")
                ),
                evidenceFingerprint,
                accountBindingFingerprint,
                observedAt,
                validUntil,
                completedAt,
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

    @Override
    public String toString() {
        return "VerifyCustomerResult[verificationId=" + verificationId
                + ", outcome=" + outcome
                + ", checks=" + checks
                + ", evidenceFingerprint=[PROTECTED]"
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", observedAt=" + observedAt
                + ", validUntil=" + validUntil
                + ", completedAt=" + completedAt
                + ", customerReference=[PROTECTED]"
                + ", accountReference=[PROTECTED]"
                + ", identity=[PROTECTED]"
                + ", account=[PROTECTED]"
                + "]";
    }

    private static void requireVerifiedBankingContext(
            String customerReference,
            String accountReference,
            VerifiedBankingIdentity identity,
            VerifiedBankingAccount account
    ) {
        if (customerReference == null
                || customerReference.isBlank()
                || accountReference == null
                || accountReference.isBlank()
                || identity == null
                || account == null) {
            throw new CustomerVerificationDomainException(
                    "VERIFIED customer result requires canonical banking "
                            + "customer/account references and KYC/contact evidence"
            );
        }

        if (!customerReference.equals(identity.customerReference())
                || !customerReference.equals(account.customerReference())
                || !accountReference.equals(account.accountReference())) {
            throw new CustomerVerificationDomainException(
                    "VERIFIED customer result banking references are inconsistent"
            );
        }
    }
}

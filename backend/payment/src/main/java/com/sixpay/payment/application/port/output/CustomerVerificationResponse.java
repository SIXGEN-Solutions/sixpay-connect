package com.sixpay.payment.application.port.output;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment-native response returned by the Customer Verification output port.
 *
 * <p>This is not yet a Payment domain snapshot. Conversion to
 * BankingVerificationSnapshot belongs to Lot 4.4.4.</p>
 */
public record CustomerVerificationResponse(
        UUID verificationId,
        Outcome outcome,
        List<Check> checks,
        String evidenceFingerprint,
        String accountBindingFingerprint,
        Instant observedAt,
        Instant validUntil,
        Instant completedAt
) {

    public CustomerVerificationResponse {
        verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        outcome = Objects.requireNonNull(
                outcome,
                "outcome is required"
        );
        checks = List.copyOf(
                Objects.requireNonNull(
                        checks,
                        "checks are required"
                )
        );
        evidenceFingerprint = requireText(
                evidenceFingerprint,
                "evidenceFingerprint"
        );
        accountBindingFingerprint = requireText(
                accountBindingFingerprint,
                "accountBindingFingerprint"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );
        completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt is required"
        );

        validateChecks(checks);
        validateOutcome(outcome, checks);

        if (validUntil != null
                && validUntil.isBefore(observedAt)) {
            throw new IllegalArgumentException(
                    "validUntil must not be before observedAt"
            );
        }
        if (completedAt.isBefore(observedAt)) {
            throw new IllegalArgumentException(
                    "completedAt must not be before observedAt"
            );
        }
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }

    private static void validateChecks(List<Check> checks) {
        if (checks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "checks must not contain null"
            );
        }

        Set<CheckType> actual = checks.stream()
                .map(Check::type)
                .collect(Collectors.toSet());

        if (actual.size() != checks.size()) {
            throw new IllegalArgumentException(
                    "verification check types must be unique"
            );
        }

        if (!actual.equals(EnumSet.allOf(CheckType.class))) {
            throw new IllegalArgumentException(
                    "all canonical verification checks are required"
            );
        }
    }

    private static void validateOutcome(
            Outcome outcome,
            List<Check> checks
    ) {
        boolean hasFail = checks.stream()
                .anyMatch(check ->
                        check.result() == CheckResult.FAIL
                );
        boolean hasUnknown = checks.stream()
                .anyMatch(check ->
                        check.result() == CheckResult.UNKNOWN
                );

        Outcome derived = hasFail
                ? Outcome.REJECTED
                : hasUnknown
                        ? Outcome.INDETERMINATE
                        : Outcome.VERIFIED;

        if (outcome != derived) {
            throw new IllegalArgumentException(
                    "outcome is inconsistent with checks"
            );
        }
    }

    private static String requireText(
            String value,
            String name
    ) {
        Objects.requireNonNull(value, name + " is required");

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be blank"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "CustomerVerificationResponse[verificationId="
                + verificationId
                + ", outcome="
                + outcome
                + ", checks="
                + checks
                + ", evidenceFingerprint=[PROTECTED]"
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", observedAt="
                + observedAt
                + ", validUntil="
                + validUntil
                + ", completedAt="
                + completedAt
                + "]";
    }

    public enum Outcome {
        VERIFIED,
        REJECTED,
        INDETERMINATE
    }

    public enum CheckResult {
        PASS,
        FAIL,
        UNKNOWN
    }

    public enum CheckType {
        CUSTOMER_EXISTS,
        FINANCIAL_INSTITUTION_MATCHES,
        NIU_MATCHES,
        IDENTITY_MATCHES,
        ACCOUNT_EXISTS,
        ACCOUNT_BELONGS_TO_CUSTOMER,
        ACCOUNT_IS_ACTIVE,
        ACCOUNT_NOT_BLOCKED,
        ACCOUNT_NOT_OPPOSED,
        REQUIRED_KYC_PRESENT,
        REQUIRED_KYC_VERIFIED
    }

    public record Check(
            CheckType type,
            CheckResult result,
            String failureCode
    ) {
        public Check {
            type = Objects.requireNonNull(
                    type,
                    "type is required"
            );
            result = Objects.requireNonNull(
                    result,
                    "result is required"
            );

            if (failureCode != null) {
                failureCode = failureCode.strip();

                if (failureCode.isEmpty()) {
                    failureCode = null;
                }
            }

            if (result == CheckResult.PASS
                    && failureCode != null) {
                throw new IllegalArgumentException(
                        "PASS check must not contain failureCode"
                );
            }

            if (result == CheckResult.FAIL
                    && failureCode == null) {
                throw new IllegalArgumentException(
                        "FAIL check requires failureCode"
                );
            }
        }
    }
}

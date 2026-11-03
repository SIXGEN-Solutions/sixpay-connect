package com.sixpay.payment.application.port.output;

import com.sixpay.payment.application.service.ResolvedDebtorAccountReferenceFactory;
import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String customerReference,
        String accountReference,
        String accountFinancialInstitutionCode,
        String maskedAccountIdentifier,
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
        accountBindingFingerprint = normalizeOptional(
                accountBindingFingerprint
        );
        customerReference = normalizeOptional(
                customerReference
        );
        accountReference = normalizeOptional(
                accountReference
        );
        accountFinancialInstitutionCode = normalizeOptional(
                accountFinancialInstitutionCode
        );
        maskedAccountIdentifier = normalizeOptional(
                maskedAccountIdentifier
        );
        if (outcome == Outcome.VERIFIED
                && (customerReference == null
                || accountReference == null
                || accountFinancialInstitutionCode == null
                || maskedAccountIdentifier == null)) {
            throw new IllegalArgumentException(
                    "VERIFIED customer verification requires canonical "
                            + "customerReference and accountReference"
            );
        }
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

    public Optional<String> customerReferenceOptional() {
        return Optional.ofNullable(customerReference);
    }

    public Optional<String> accountReferenceOptional() {
        return Optional.ofNullable(accountReference);
    }

    public Optional<Instant> validUntilOptional() {
        return Optional.ofNullable(validUntil);
    }

    private static final Logger log = LoggerFactory.getLogger(CustomerVerificationResponse.class);

    private static void validateChecks_bk(List<Check> checks) {
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

        EnumSet<CheckType> mandatory =
                EnumSet.allOf(CheckType.class);

        //mandatory.remove(CheckType.IDENTITY_MATCHES);
        mandatory.remove(CheckType.REQUIRED_KYC_PRESENT);
        mandatory.remove(CheckType.REQUIRED_KYC_VERIFIED);

        log.info(
                "event=customer_verification_checks "
                        + "actual={} mandatory={} missing={}",
                actual.stream().sorted().toList(),
                mandatory.stream().sorted().toList(),
                mandatory.stream()
                        .filter(check -> !actual.contains(check))
                        .sorted()
                        .toList()
        );

        if (!actual.containsAll(mandatory)) {
            throw new IllegalArgumentException(
                    "mandatory verification checks are missing"
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

        System.out.println("outcome: " + outcome);
        System.out.println("derived: " + derived);
        if (outcome != derived) {
            throw new IllegalArgumentException(
                    "outcome is inconsistent with checks"
            );
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
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
                + ", customerReference=[PROTECTED]"
                + ", accountReference=[PROTECTED]"
                + ", accountFinancialInstitutionCode="
                + accountFinancialInstitutionCode
                + ", maskedAccountIdentifier=[PROTECTED]"
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

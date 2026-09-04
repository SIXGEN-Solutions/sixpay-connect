package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.infrastructure.banking.dto.*;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AmplitudeResponseValidator {

    public AmplitudeResponseValidator() {
    }

    public AmplitudeResponseValidator(
            com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties ignored
    ) {
        this();
    }

    private static final Set<String> OUTCOMES =
            Set.of("VERIFIED", "REJECTED", "INDETERMINATE");

    private static final Set<String> CHECK_RESULTS =
            Set.of("PASS", "FAIL", "UNKNOWN");

    private static final Set<String> REQUIRED_CHECK_TYPES = Set.of(
            "CUSTOMER_EXISTS",
            "FINANCIAL_INSTITUTION_MATCHES",
            "NIU_MATCHES",
            "IDENTITY_MATCHES",
            "ACCOUNT_EXISTS",
            "ACCOUNT_BELONGS_TO_CUSTOMER",
            "ACCOUNT_IS_ACTIVE",
            "ACCOUNT_NOT_BLOCKED",
            "ACCOUNT_NOT_OPPOSED",
            "REQUIRED_KYC_PRESENT",
            "REQUIRED_KYC_VERIFIED"
    );

    public AmplitudeCustomerVerificationResponse validate(
            AmplitudeCustomerVerificationResponse response
    ) {
        if (response == null) {
            throw invalid("Amplitude response body is empty");
        }

        Objects.requireNonNull(
                response.verificationId(),
                "Amplitude verificationId is required"
        );
        Objects.requireNonNull(
                response.verifiedAt(),
                "Amplitude verifiedAt is required"
        );

        if (!"AMPLITUDE".equals(required(response.source(), "source"))) {
            throw invalid("Unsupported Amplitude source");
        }

        String outcome = required(response.outcome(), "outcome");
        if (!OUTCOMES.contains(outcome)) {
            throw invalid("Unsupported Amplitude verification outcome");
        }

        List<AmplitudeVerificationCheckResponse> checks =
                response.checks();
        if (checks == null || checks.isEmpty()) {
            throw invalid("Amplitude checks are required");
        }

        Set<String> observedTypes = new HashSet<>();
        for (AmplitudeVerificationCheckResponse check : checks) {
            if (check == null) {
                throw invalid("Amplitude verification check is required");
            }

            String type = required(check.type(), "check type");
            if (!observedTypes.add(type)) {
                throw invalid(
                        "Amplitude verification checks must be unique by type"
                );
            }

            if (!REQUIRED_CHECK_TYPES.contains(type)) {
                throw invalid(
                        "Unsupported Amplitude verification check type"
                );
            }

            String result = required(check.result(), "check result");
            if (!CHECK_RESULTS.contains(result)) {
                throw invalid(
                        "Unsupported Amplitude verification check result"
                );
            }
        }

        if (!observedTypes.equals(REQUIRED_CHECK_TYPES)) {
            Set<String> missing = new HashSet<>(REQUIRED_CHECK_TYPES);
            missing.removeAll(observedTypes);
            throw invalid(
                    "Amplitude verification checks are incomplete; missing=" + missing
            );
        }

        if ("VERIFIED".equals(outcome)) {
            validateVerifiedResponse(response);
        }

        return response;
    }

    private static void validateVerifiedResponse(
            AmplitudeCustomerVerificationResponse response
    ) {
        String customerReference = rawRequired(
                response.customerReference(),
                "customerReference"
        );
        String accountReference = rawRequired(
                response.accountReference(),
                "accountReference"
        );

        AmplitudeCustomerIdentityResponse identity =
                Objects.requireNonNull(
                        response.identity(),
                        "Amplitude identity is required for VERIFIED outcome"
                );
        AmplitudeBankAccountResponse account =
                Objects.requireNonNull(
                        response.account(),
                        "Amplitude account is required for VERIFIED outcome"
                );

        if (!customerReference.equals(identity.customerReference())
                || !customerReference.equals(account.customerReference())
                || !accountReference.equals(account.accountReference())) {
            throw invalid(
                    "Amplitude VERIFIED banking references are inconsistent"
            );
        }

        if (!"AMPLITUDE".equals(required(identity.source(), "identity source"))
                || !"AMPLITUDE".equals(
                required(account.source(), "account source")
        )) {
            throw invalid("Amplitude VERIFIED nested source is invalid");
        }

        if (identity.kycFields() == null) {
            throw invalid(
                    "Amplitude VERIFIED identity kycFields are required"
            );
        }
    }

    private static String required(
            String value,
            String name
    ) {
        return rawRequired(value, name).toUpperCase(Locale.ROOT);
    }

    private static String rawRequired(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw invalid("Amplitude " + name + " is required");
        }
        return value.strip();
    }

    private static AmplitudeInvalidResponseException invalid(
            String message
    ) {
        return new AmplitudeInvalidResponseException(message);
    }
}

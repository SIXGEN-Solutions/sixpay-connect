package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AmplitudeResponseValidator {

    private final Set<String> successCodes;
    private final Set<String> businessFailureCodes;

    public AmplitudeResponseValidator(
            BankingVerificationProperties properties
    ) {
        Objects.requireNonNull(properties);
        this.successCodes = normalized(
                properties.contract().successCodes()
        );
        this.businessFailureCodes = normalized(
                properties.contract().businessFailureCodes()
        );
    }

    public AmplitudeCustomerVerificationResponse validate(
            AmplitudeCustomerVerificationResponse response
    ) {
        if (response == null) {
            throw new AmplitudeInvalidResponseException(
                    "Amplitude response body is empty"
            );
        }

        String code = required(response.code(), "code");
        String result = required(response.result(), "result")
                .toUpperCase(Locale.ROOT);

        if (!successCodes.contains(code)
                && !businessFailureCodes.contains(code)) {
            throw new AmplitudeInvalidResponseException(
                    "Unsupported Amplitude functional code"
            );
        }

        if (!Set.of("SUCCESS", "FAILURE", "REJECTED")
                .contains(result)) {
            throw new AmplitudeInvalidResponseException(
                    "Unsupported Amplitude result"
            );
        }

        Instant observedAt = response.observedAt();
        if (observedAt == null) {
            throw new AmplitudeInvalidResponseException(
                    "Amplitude observedAt is required"
            );
        }

        Map<String, String> checks = response.checks();
        if (checks == null || checks.isEmpty()) {
            throw new AmplitudeInvalidResponseException(
                    "Amplitude checks are required"
            );
        }

        checks.forEach((type, value) -> {
            required(type, "check type");
            String normalized = required(value, "check result")
                    .toUpperCase(Locale.ROOT);
            if (!Set.of("PASS", "FAIL", "UNKNOWN")
                    .contains(normalized)) {
                throw new AmplitudeInvalidResponseException(
                        "Unsupported Amplitude verification check result"
                );
            }
        });

        return response;
    }

    private static Set<String> normalized(
            Set<String> values
    ) {
        return values.stream()
                .map(value -> required(value, "code"))
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new AmplitudeInvalidResponseException(
                    "Amplitude " + name + " is required"
            );
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
}

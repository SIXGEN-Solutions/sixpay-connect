package com.sixpay.payment.infrastructure.banking.amplitude.validation;

import com.sixpay.payment.infrastructure.banking.amplitude.configuration.AmplitudePaymentBankingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.dto.*;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AmplitudeAccountFundsResponseValidator {

    private final Set<String> acceptedCodes;

    public AmplitudeAccountFundsResponseValidator(
            AmplitudePaymentBankingProperties properties
    ) {
        Objects.requireNonNull(properties);
        acceptedCodes = java.util.stream.Stream.concat(
                        properties.contract()
                                .successCodes()
                                .stream(),
                        properties.contract()
                                .rejectionCodes()
                                .stream()
                )
                .map(AmplitudeAccountFundsResponseValidator::normalize)
                .collect(
                        java.util.stream.Collectors.toUnmodifiableSet()
                );
    }

    public AmplitudeAccountVerificationResponse validate(
            AmplitudeAccountVerificationResponse response
    ) {
        Objects.requireNonNull(
                response,
                "Amplitude verification response is empty"
        );
        validateCode(response.code());
        required(response.verificationId(), "verificationId");
        required(response.outcome(), "outcome");
        required(
                response.accountBindingFingerprint(),
                "accountBindingFingerprint"
        );
        validateChecks(response.checks());
        Objects.requireNonNull(
                response.observedAt(),
                "Amplitude observedAt is required"
        );
        return response;
    }

    public AmplitudeFundsCheckResponse validate(
            AmplitudeFundsCheckResponse response
    ) {
        Objects.requireNonNull(
                response,
                "Amplitude funds response is empty"
        );
        validateCode(response.code());
        required(
                response.verificationReference(),
                "verificationReference"
        );
        required(response.outcome(), "outcome");
        required(response.currency(), "currency");
        required(
                response.accountBindingFingerprint(),
                "accountBindingFingerprint"
        );
        Objects.requireNonNull(
                response.checkedAmount(),
                "Amplitude checkedAmount is required"
        );
        Objects.requireNonNull(
                response.observedAt(),
                "Amplitude observedAt is required"
        );
        Objects.requireNonNull(
                response.validUntil(),
                "Amplitude validUntil is required"
        );
        validateChecks(response.checks());
        return response;
    }

    private void validateCode(String code) {
        if (!acceptedCodes.contains(normalize(code))) {
            throw new IllegalArgumentException(
                    "Unsupported Amplitude functional code"
            );
        }
    }

    private static void validateChecks(
            Map<String, AmplitudeCheckResult> checks
    ) {
        if (checks == null || checks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Amplitude checks are required"
            );
        }
        checks.forEach((name, result) -> {
            required(name, "check name");
            Objects.requireNonNull(
                    result,
                    "Amplitude check result"
            );
            String value = normalize(result.result());
            if (!Set.of("PASS", "FAIL", "UNKNOWN")
                    .contains(value)) {
                throw new IllegalArgumentException(
                        "Unsupported Amplitude check result"
                );
            }
        });
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Amplitude " + name + " is required"
            );
        }
        return value.strip();
    }

    private static String normalize(String value) {
        return required(value, "code")
                .toUpperCase(Locale.ROOT);
    }
}

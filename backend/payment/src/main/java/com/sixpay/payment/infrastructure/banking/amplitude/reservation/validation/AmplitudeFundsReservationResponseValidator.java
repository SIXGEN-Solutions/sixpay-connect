package com.sixpay.payment.infrastructure.banking.amplitude.reservation.validation;

import com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration.AmplitudeFundsReservationProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto.AmplitudeFundsReservationResponse;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AmplitudeFundsReservationResponseValidator {

    private final Set<String> reservedCodes;
    private final Set<String> rejectedCodes;

    public AmplitudeFundsReservationResponseValidator(
            AmplitudeFundsReservationProperties properties
    ) {
        reservedCodes = normalize(properties.contract().reservedCodes());
        rejectedCodes = normalize(properties.contract().rejectedCodes());
    }

    public AmplitudeFundsReservationResponse validate(
            AmplitudeFundsReservationResponse response
    ) {
        Objects.requireNonNull(
                response,
                "Amplitude reservation response is empty"
        );

        String code = normalized(response.code());
        String outcome = normalized(response.outcome());

        if (!reservedCodes.contains(code)
                && !rejectedCodes.contains(code)) {
            throw new IllegalArgumentException(
                    "Unsupported Amplitude reservation code"
            );
        }

        if (!Set.of("RESERVED", "REJECTED")
                .contains(outcome)) {
            throw new IllegalArgumentException(
                    "Unsupported Amplitude reservation outcome"
            );
        }

        Objects.requireNonNull(
                response.reservedAmount(),
                "Amplitude reservedAmount is required"
        );
        required(response.currency(), "currency");
        required(
                response.accountBindingFingerprint(),
                "accountBindingFingerprint"
        );
        Objects.requireNonNull(
                response.observedAt(),
                "Amplitude observedAt is required"
        );

        if ("RESERVED".equals(outcome)) {
            required(
                    response.reservationReference(),
                    "reservationReference"
            );
            Objects.requireNonNull(
                    response.expiresAt(),
                    "Amplitude expiresAt is required"
            );
            if (response.reasonCode() != null) {
                throw new IllegalArgumentException(
                        "Reserved response must not expose a reason code"
                );
            }
        } else {
            required(response.reasonCode(), "reasonCode");
        }

        return response;
    }

    private static Set<String> normalize(Set<String> values) {
        return values.stream()
                .map(AmplitudeFundsReservationResponseValidator::normalized)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalized(String value) {
        return required(value, "value")
                .toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Amplitude " + name + " is required"
            );
        }
        return value.strip();
    }
}

package com.sixpay.payment.infrastructure.banking.amplitude.posting.validation;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePostingResponse;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AmplitudePostingResponseValidator {

    private final Set<String> acceptedCodes;

    public AmplitudePostingResponseValidator(
            AmplitudePostingProperties properties
    ) {
        acceptedCodes = java.util.stream.Stream.of(
                        properties.contract().completedCodes(),
                        properties.contract().rejectedCodes(),
                        properties.contract().pendingCodes()
                )
                .flatMap(Set::stream)
                .map(AmplitudePostingResponseValidator::normalized)
                .collect(Collectors.toUnmodifiableSet());
    }

    public AmplitudePostingResponse validate(
            AmplitudePostingResponse response
    ) {
        Objects.requireNonNull(
                response,
                "Amplitude posting response is empty"
        );

        if (!acceptedCodes.contains(
                normalized(response.code())
        )) {
            throw new IllegalArgumentException(
                    "Unsupported Amplitude posting code"
            );
        }

        required(
                response.postingInstructionId(),
                "postingInstructionId"
        );
        required(
                response.postingIdempotencyKey(),
                "postingIdempotencyKey"
        );
        required(response.outcome(), "outcome");
        required(
                response.debitLegStatus(),
                "debitLegStatus"
        );
        required(
                response.cutCreditLegStatus(),
                "cutCreditLegStatus"
        );
        required(response.nextAction(), "nextAction");

        return response;
    }

    private static String normalized(String value) {
        return required(value, "value")
                .toUpperCase(Locale.ROOT);
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
}

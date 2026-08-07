package com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(
        prefix = AmplitudeFundsReservationProperties.PREFIX
)
public record AmplitudeFundsReservationProperties(
        @NotNull URI baseUrl,
        @NotBlank String reservationPath,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Valid Security security,
        @NotNull @Valid Contract contract
) {
    public static final String PREFIX =
            "sixpay.payment.banking.amplitude.reservation";

    public AmplitudeFundsReservationProperties {
        validateBaseUrl(baseUrl);
        reservationPath = path(
                reservationPath,
                "reservationPath"
        );
        connectTimeout = positive(
                connectTimeout,
                "connectTimeout"
        );
        readTimeout = positive(
                readTimeout,
                "readTimeout"
        );
    }

    public record Security(
            @NotBlank String oauth2RegistrationId,
            @NotBlank String sslBundle
    ) {
        public Security {
            oauth2RegistrationId = text(
                    oauth2RegistrationId,
                    "oauth2RegistrationId"
            );
            sslBundle = text(
                    sslBundle,
                    "sslBundle"
            );
        }
    }

    public record Contract(
            @NotBlank String version,
            @NotNull Set<@NotBlank String> reservedCodes,
            @NotNull Set<@NotBlank String> rejectedCodes,
            @NotBlank String idempotencyHeader
    ) {
        public Contract {
            version = text(version, "version");
            reservedCodes = Set.copyOf(reservedCodes);
            rejectedCodes = Set.copyOf(rejectedCodes);
            idempotencyHeader = text(
                    idempotencyHeader,
                    "idempotencyHeader"
            );

            if (reservedCodes.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one reservation success code is required"
                );
            }
        }
    }

    private static void validateBaseUrl(URI value) {
        if (value == null
                || value.getScheme() == null
                || value.getHost() == null) {
            throw new IllegalArgumentException(
                    "baseUrl must be absolute"
            );
        }

        boolean https =
                "https".equalsIgnoreCase(
                        value.getScheme()
                );

        boolean loopback =
                "http".equalsIgnoreCase(
                        value.getScheme()
                )
                && (
                "localhost".equalsIgnoreCase(
                        value.getHost()
                )
                        || "127.0.0.1".equals(
                        value.getHost()
                )
                        || "::1".equals(
                        value.getHost()
                )
        );

        if (!https && !loopback) {
            throw new IllegalArgumentException(
                    "baseUrl must use HTTPS except for loopback tests"
            );
        }
    }

    private static String path(
            String value,
            String name
    ) {
        String result = text(value, name);
        if (!result.startsWith("/")) {
            throw new IllegalArgumentException(
                    name + " must start with /"
            );
        }
        return result;
    }

    private static String text(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }

    private static Duration positive(
            Duration value,
            String name
    ) {
        if (value == null
                || value.isZero()
                || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must be positive"
            );
        }
        return value;
    }
}

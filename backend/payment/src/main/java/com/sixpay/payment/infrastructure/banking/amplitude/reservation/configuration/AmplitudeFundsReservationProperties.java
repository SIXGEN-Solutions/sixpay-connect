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
            boolean oauth2Enabled,
            boolean mtlsEnabled,
            boolean allowInsecureHttp,
            String oauth2RegistrationId,
            String sslBundle
    ) {
        @org.springframework.boot.context.properties.bind.ConstructorBinding
        public Security {
            if (oauth2Enabled && (oauth2RegistrationId == null
                    || oauth2RegistrationId.isBlank())) {
                throw new IllegalArgumentException(
                        "oauth2RegistrationId is required when OAuth2 is enabled");
            }
            if (mtlsEnabled && (sslBundle == null || sslBundle.isBlank())) {
                throw new IllegalArgumentException(
                        "sslBundle is required when mTLS is enabled");
            }
            if (allowInsecureHttp && (oauth2Enabled || mtlsEnabled)) {
                throw new IllegalArgumentException(
                        "insecure HTTP cannot be combined with OAuth2 or mTLS");
            }
            if (oauth2RegistrationId != null) {
                oauth2RegistrationId = oauth2RegistrationId.strip();
            }
            if (sslBundle != null) {
                sslBundle = sslBundle.strip();
            }
        }

        // Retains existing direct constructor usages. Default is OAuth2 + mTLS.
        public Security(String oauth2RegistrationId, String sslBundle) {
            this(true, true, false, oauth2RegistrationId, sslBundle);
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

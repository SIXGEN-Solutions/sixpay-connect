package com.sixpay.payment.infrastructure.banking.amplitude.status.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(
        prefix = AmplitudePostingStatusProperties.PREFIX
)
public record AmplitudePostingStatusProperties(
        boolean enabled,
        URI baseUrl,
        String byIdempotencyPath,
        String byBankReferencePath,
        Duration connectTimeout,
        Duration readTimeout,
        Security security
) {
    public static final String PREFIX =
            "sixpay.payment.banking.amplitude.status";

    public AmplitudePostingStatusProperties {
        validateBaseUrl(baseUrl);
        byIdempotencyPath = path(
                byIdempotencyPath,
                "byIdempotencyPath"
        );
        byBankReferencePath = path(
                byBankReferencePath,
                "byBankReferencePath"
        );
        connectTimeout = positive(
                connectTimeout,
                "connectTimeout"
        );
        readTimeout = positive(
                readTimeout,
                "readTimeout"
        );
        security = Objects.requireNonNull(
                security,
                "security"
        );
    }

    public record Security(
            String oauth2RegistrationId,
            String sslBundle
    ) {
        public Security {
            oauth2RegistrationId = required(
                    oauth2RegistrationId,
                    "oauth2RegistrationId"
            );
            sslBundle = required(
                    sslBundle,
                    "sslBundle"
            );
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
                "https".equalsIgnoreCase(value.getScheme());

        boolean loopback =
                "http".equalsIgnoreCase(value.getScheme())
                        && ("localhost".equalsIgnoreCase(
                        value.getHost()
                )
                        || "127.0.0.1".equals(
                        value.getHost()
                )
                        || "::1".equals(
                        value.getHost()
                ));

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
        String result = required(value, name);

        if (!result.startsWith("/")) {
            throw new IllegalArgumentException(
                    name + " must start with /"
            );
        }

        return result;
    }

    private static String required(
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

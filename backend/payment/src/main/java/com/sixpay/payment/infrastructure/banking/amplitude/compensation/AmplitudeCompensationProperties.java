package com.sixpay.payment.infrastructure.banking.amplitude.compensation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@ConfigurationProperties(prefix = AmplitudeCompensationProperties.PREFIX)
public record AmplitudeCompensationProperties(
        boolean enabled,
        URI baseUrl,
        String releasePath,
        String reversalPath,
        Duration connectTimeout,
        Duration readTimeout,
        Security security,
        Contract contract
) {
    public static final String PREFIX =
            "sixpay.payment.banking.amplitude.compensation";

    public AmplitudeCompensationProperties {
        if (baseUrl == null
                || baseUrl.getScheme() == null
                || baseUrl.getHost() == null) {
            throw new IllegalArgumentException(
                    "baseUrl must be absolute"
            );
        }

        boolean secure =
                "https".equalsIgnoreCase(baseUrl.getScheme());
        boolean loopback =
                "http".equalsIgnoreCase(baseUrl.getScheme())
                        && ("localhost".equalsIgnoreCase(baseUrl.getHost())
                        || "127.0.0.1".equals(baseUrl.getHost())
                        || "::1".equals(baseUrl.getHost()));

        if (!secure && !loopback) {
            throw new IllegalArgumentException(
                    "baseUrl must use HTTPS except for loopback tests"
            );
        }

        releasePath = path(releasePath, "releasePath");
        reversalPath = path(reversalPath, "reversalPath");
        connectTimeout = positive(
                connectTimeout,
                "connectTimeout"
        );
        readTimeout = positive(readTimeout, "readTimeout");
        security = java.util.Objects.requireNonNull(
                security,
                "security"
        );
        contract = java.util.Objects.requireNonNull(
                contract,
                "contract"
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
            sslBundle = required(sslBundle, "sslBundle");
        }
    }

    public record Contract(
            String version,
            String idempotencyHeader,
            Set<String> releaseSuccessCodes,
            Set<String> releaseRejectedCodes,
            Set<String> reversalSuccessCodes,
            Set<String> reversalRejectedCodes,
            Set<String> reversalNotAllowedCodes
    ) {
        public Contract {
            version = required(version, "version");
            idempotencyHeader = required(
                    idempotencyHeader,
                    "idempotencyHeader"
            );
            releaseSuccessCodes =
                    Set.copyOf(releaseSuccessCodes);
            releaseRejectedCodes =
                    Set.copyOf(releaseRejectedCodes);
            reversalSuccessCodes =
                    Set.copyOf(reversalSuccessCodes);
            reversalRejectedCodes =
                    Set.copyOf(reversalRejectedCodes);
            reversalNotAllowedCodes =
                    Set.copyOf(reversalNotAllowedCodes);
        }
    }

    private static String path(String value, String name) {
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

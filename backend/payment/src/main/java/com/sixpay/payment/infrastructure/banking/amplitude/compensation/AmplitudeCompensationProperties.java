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

package com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = AmplitudePostingProperties.PREFIX)
public record AmplitudePostingProperties(
        @NotNull URI baseUrl,
        @NotBlank String postingPath,
        @NotBlank String paymentReferenceLookupPath,
        @NotBlank String idempotencyLookupPath,
        @NotBlank String allocateEventNumberPath,
        @NotBlank String accountingDatePath,
        @NotBlank String nightModePath,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Valid Security security,
        @NotNull @Valid Contract contract
) {
    public static final String PREFIX =
            "sixpay.payment.banking.amplitude.posting";

    public AmplitudePostingProperties {
        if (baseUrl == null
                || baseUrl.getScheme() == null
                || baseUrl.getHost() == null) {
            throw new IllegalArgumentException(
                    "baseUrl must be absolute"
            );
        }
        boolean https =
                "https".equalsIgnoreCase(baseUrl.getScheme());
        boolean loopback =
                "http".equalsIgnoreCase(baseUrl.getScheme())
                && ("localhost".equalsIgnoreCase(baseUrl.getHost())
                || "127.0.0.1".equals(baseUrl.getHost())
                || "::1".equals(baseUrl.getHost()));
        if (!https && !loopback) {
            throw new IllegalArgumentException(
                    "baseUrl must use HTTPS except for loopback tests"
            );
        }
        postingPath = validatePath(postingPath, "postingPath");
        paymentReferenceLookupPath = validatePath(paymentReferenceLookupPath, "paymentReferenceLookupPath");
        idempotencyLookupPath = validatePath(idempotencyLookupPath, "idempotencyLookupPath");
        allocateEventNumberPath = validatePath(allocateEventNumberPath, "allocateEventNumberPath");
        accountingDatePath = validatePath(accountingDatePath, "accountingDatePath");
        nightModePath = validatePath(nightModePath, "nightModePath");
        if (connectTimeout == null
                || connectTimeout.isZero()
                || connectTimeout.isNegative()
                || readTimeout == null
                || readTimeout.isZero()
                || readTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Timeouts must be positive"
            );
        }
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
            @NotBlank String idempotencyHeader,
            @NotBlank String correlationHeader,
            @NotBlank String institutionHeader
    ) { }

    private static String validatePath(String value, String name) {
        if (value == null || value.isBlank() || !value.startsWith("/")) {
            throw new IllegalArgumentException(name + " must start with /");
        }
        return value.strip();
    }
}

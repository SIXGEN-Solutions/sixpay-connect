package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = AmplitudePaymentConfirmationProperties.PREFIX)
public record AmplitudePaymentConfirmationProperties(
        boolean enabled,
        @NotNull URI baseUrl,
        @NotBlank String createPath,
        @NotBlank String challengePath,
        @NotBlank String verificationPath,
        @NotBlank String replacementPath,
        @NotBlank String lookupByIdempotencyPath,
        @NotBlank String revocationPath,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @NotNull @Valid Security security,
        @NotNull @Valid Contract contract
) {
    public static final String PREFIX = "sixpay.payment.banking.amplitude.confirmation";

    public AmplitudePaymentConfirmationProperties {
        if (baseUrl == null || baseUrl.getScheme() == null || baseUrl.getHost() == null) {
            throw new IllegalArgumentException("baseUrl must be absolute");
        }
        boolean https = "https".equalsIgnoreCase(baseUrl.getScheme());
        boolean allowedHttp = security != null && security.allowInsecureHttp()
                && "http".equalsIgnoreCase(baseUrl.getScheme());
        if (!https && !allowedHttp) {
            throw new IllegalArgumentException("baseUrl must use HTTPS unless insecure HTTP is explicitly enabled");
        }
        validatePath(createPath, "createPath");
        validatePath(challengePath, "challengePath");
        validatePath(verificationPath, "verificationPath");
        validatePath(replacementPath, "replacementPath");
        validatePath(lookupByIdempotencyPath, "lookupByIdempotencyPath");
        validatePath(revocationPath, "revocationPath");
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("Timeouts must be positive");
        }
    }

    private static void validatePath(String value, String name) {
        if (value == null || value.isBlank() || !value.startsWith("/")) {
            throw new IllegalArgumentException(name + " must start with /");
        }
    }

    public record Security(boolean oauth2Enabled, boolean mtlsEnabled, boolean allowInsecureHttp,
                           String oauth2RegistrationId, String sslBundle) {
        public Security {
            if (oauth2Enabled && (oauth2RegistrationId == null || oauth2RegistrationId.isBlank()))
                throw new IllegalArgumentException("oauth2RegistrationId is required when OAuth2 is enabled");
            if (mtlsEnabled && (sslBundle == null || sslBundle.isBlank()))
                throw new IllegalArgumentException("sslBundle is required when mTLS is enabled");
            if (allowInsecureHttp && (oauth2Enabled || mtlsEnabled))
                throw new IllegalArgumentException("insecure HTTP cannot be combined with OAuth2 or mTLS");
        }
    }
    public record Contract(
            @NotBlank String idempotencyHeader,
            @NotBlank String correlationHeader,
            @NotBlank String institutionHeader
    ) { }
}

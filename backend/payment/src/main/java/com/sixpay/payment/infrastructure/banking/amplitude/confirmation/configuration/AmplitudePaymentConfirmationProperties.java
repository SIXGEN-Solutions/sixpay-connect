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
        boolean loopback = "http".equalsIgnoreCase(baseUrl.getScheme())
                && ("localhost".equalsIgnoreCase(baseUrl.getHost())
                || "127.0.0.1".equals(baseUrl.getHost())
                || "::1".equals(baseUrl.getHost()));
        if (!https && !loopback) {
            throw new IllegalArgumentException("baseUrl must use HTTPS except for loopback tests");
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

    public record Security(@NotBlank String oauth2RegistrationId, @NotBlank String sslBundle) { }
    public record Contract(
            @NotBlank String idempotencyHeader,
            @NotBlank String correlationHeader,
            @NotBlank String institutionHeader
    ) { }
}

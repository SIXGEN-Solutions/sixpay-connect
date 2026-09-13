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
            @NotBlank String oauth2RegistrationId,
            @NotBlank String sslBundle
    ) { }

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

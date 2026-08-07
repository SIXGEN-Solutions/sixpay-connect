package com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = AmplitudePostingProperties.PREFIX)
public record AmplitudePostingProperties(
        @NotNull URI baseUrl,
        @NotBlank String postingPath,
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
        if (postingPath == null
                || postingPath.isBlank()
                || !postingPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "postingPath must start with /"
            );
        }
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
            @NotNull Set<@NotBlank String> completedCodes,
            @NotNull Set<@NotBlank String> rejectedCodes,
            @NotNull Set<@NotBlank String> pendingCodes
    ) {
        public Contract {
            completedCodes = Set.copyOf(completedCodes);
            rejectedCodes = Set.copyOf(rejectedCodes);
            pendingCodes = Set.copyOf(pendingCodes);
            if (completedCodes.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one completed code is required"
                );
            }
        }
    }
}

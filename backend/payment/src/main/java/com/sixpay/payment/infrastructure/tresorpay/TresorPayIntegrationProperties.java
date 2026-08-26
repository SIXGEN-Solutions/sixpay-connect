package com.sixpay.payment.infrastructure.tresorpay;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "sixpay.payment.tresorpay")
public record TresorPayIntegrationProperties(
        @NotNull @Valid Security security,
        @NotNull @Valid AntiReplay antiReplay,
        @NotNull @Valid RateLimit rateLimit,
        @NotNull @Valid Callback callback,
        @NotNull List<@NotBlank String> allowedCallbackHosts
) {
    public TresorPayIntegrationProperties {
        allowedCallbackHosts = allowedCallbackHosts == null
                ? List.of()
                : allowedCallbackHosts.stream().map(String::strip).toList();
    }

    public record Security(
            boolean mtlsRequired,
            boolean oauth2Required,
            boolean apiKeyEnabled,
            String apiKeyHeader,
            String apiKeyValue,
            @NotBlank String audience,
            @NotBlank String partnerClaim,
            @NotBlank String requiredScope
    ) {
        public Security {
            apiKeyHeader = optional(apiKeyHeader, "X-API-Key");
            apiKeyValue = optional(apiKeyValue, null);
            audience = required(audience, "audience");
            partnerClaim = required(partnerClaim, "partnerClaim");
            requiredScope = required(requiredScope, "requiredScope");

            if (apiKeyEnabled && apiKeyValue == null) {
                throw new IllegalArgumentException(
                        "API key value is required when API key is enabled"
                );
            }
        }
    }

    public record AntiReplay(
            boolean enabled,
            @NotNull Duration allowedClockSkew,
            @NotNull Duration nonceTtl
    ) {
        public AntiReplay {
            allowedClockSkew = positive(
                    allowedClockSkew,
                    "allowedClockSkew"
            );
            nonceTtl = positive(nonceTtl, "nonceTtl");
        }
    }

    public record RateLimit(
            boolean enabled,
            @Min(1) int requestsPerMinute
    ) { }

    public record Callback(
            boolean signatureEnabled,
            @NotBlank String algorithm,
            @NotNull Duration deliveryExpiration
    ) {
        public Callback {
            algorithm = required(algorithm, "algorithm");
            deliveryExpiration = positive(
                    deliveryExpiration,
                    "deliveryExpiration"
            );
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static String optional(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value.strip();
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}

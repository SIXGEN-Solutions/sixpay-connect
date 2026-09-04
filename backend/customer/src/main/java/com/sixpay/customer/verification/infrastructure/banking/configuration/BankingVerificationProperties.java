package com.sixpay.customer.verification.infrastructure.banking.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

@Validated
@ConfigurationProperties(
        prefix = BankingVerificationProperties.PREFIX
)
public record BankingVerificationProperties(
        @NotNull URI baseUrl,
        @NotBlank String endpointPath,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(1) @Max(5) int maxAttempts,
        @NotNull Duration retryBackoff,
        @NotNull Duration evidenceTtl,
        @NotNull @Valid Security security,
        @NotNull @Valid Contract contract
) {
    public static final String PREFIX =
            "sixpay.customer.verification.banking";

    public static final String DEFAULT_ENDPOINT_PATH =
            "/api/v1/customer-verifications";

    public BankingVerificationProperties {
        endpointPath = required(endpointPath, "endpointPath");
        if (!endpointPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "endpointPath must start with /"
            );
        }
        validateBaseUrl(baseUrl);
        connectTimeout = positive(
                connectTimeout,
                "connectTimeout"
        );
        readTimeout = positive(
                readTimeout,
                "readTimeout"
        );
        if (readTimeout.compareTo(connectTimeout) < 0) {
            throw new IllegalArgumentException(
                    "readTimeout must be >= connectTimeout"
            );
        }
        retryBackoff = positive(
                retryBackoff,
                "retryBackoff"
        );
        evidenceTtl = positive(
                evidenceTtl,
                "evidenceTtl"
        );
    }

    public record Security(
            @NotBlank String oauth2RegistrationId,
            @NotBlank String sslBundle
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
            @NotBlank String version,
            @NotNull Set<@NotBlank String> successCodes,
            @NotNull Set<@NotBlank String> businessFailureCodes
    ) {
        public Contract {
            version = required(version, "version");
            successCodes = Set.copyOf(Objects.requireNonNull(successCodes, "successCodes are required"));
            businessFailureCodes = Set.copyOf(Objects.requireNonNull(businessFailureCodes, "businessFailureCodes are required"));
            if (successCodes.isEmpty()) {
                throw new IllegalArgumentException("At least one Amplitude success code is required");
            }
        }

        public Contract(String version) {
            this(version, Set.of("LEGACY"), Set.of());
        }
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

    private static void validateBaseUrl(
            URI baseUrl
    ) {
        if (baseUrl == null
                || baseUrl.getScheme() == null
                || baseUrl.getHost() == null) {
            throw new IllegalArgumentException(
                    "baseUrl must be an absolute URI"
            );
        }

        boolean https =
                "https".equalsIgnoreCase(
                        baseUrl.getScheme()
                );

        boolean localTestEndpoint =
                "http".equalsIgnoreCase(
                        baseUrl.getScheme()
                )
                        && isLoopbackHost(baseUrl.getHost());

        if (!https && !localTestEndpoint) {
            throw new IllegalArgumentException(
                    "baseUrl must use HTTPS except for a local loopback test endpoint"
            );
        }
    }

    private static boolean isLoopbackHost(
            String host
    ) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }
}

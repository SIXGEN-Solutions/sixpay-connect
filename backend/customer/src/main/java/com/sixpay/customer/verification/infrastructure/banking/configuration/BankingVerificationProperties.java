package com.sixpay.customer.verification.infrastructure.banking.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Validated technical configuration for the Core Banking verification adapter.
 *
 * <p>No secret value is modeled here. OAuth client secrets, private keys and
 * certificates are supplied through platform-managed secret configuration.</p>
 */
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
        @NotNull Security security
) {

    public static final String PREFIX =
            "sixpay.customer.verification.banking";

    public static final String DEFAULT_ENDPOINT_PATH =
            "/v1/accounts/verify";

    public BankingVerificationProperties {
        if (endpointPath != null) {
            endpointPath = endpointPath.strip();
        }
    }

    @AssertTrue(
            message = "Banking verification configuration values must be positive and consistent"
    )
    public boolean isValid() {
        return isHttps(baseUrl)
                && endpointPath != null
                && endpointPath.startsWith("/")
                && isPositive(connectTimeout)
                && isPositive(readTimeout)
                && readTimeout.compareTo(connectTimeout) >= 0
                && isPositive(retryBackoff)
                && isPositive(evidenceTtl)
                && security != null
                && security.isValid();
    }

    private static boolean isHttps(URI uri) {
        return uri != null
                && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null;
    }

    private static boolean isPositive(Duration duration) {
        return duration != null
                && !duration.isZero()
                && !duration.isNegative();
    }

    public record Security(
            @NotBlank String oauth2RegistrationId,
            @NotBlank String sslBundle
    ) {
        public Security {
            if (oauth2RegistrationId != null) {
                oauth2RegistrationId =
                        oauth2RegistrationId.strip();
            }
            if (sslBundle != null) {
                sslBundle = sslBundle.strip();
            }
        }

        public boolean isValid() {
            return oauth2RegistrationId != null
                    && !oauth2RegistrationId.isBlank()
                    && sslBundle != null
                    && !sslBundle.isBlank();
        }
    }
}

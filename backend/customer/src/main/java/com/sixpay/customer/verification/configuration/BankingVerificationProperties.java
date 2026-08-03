package com.sixpay.customer.verification.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = BankingVerificationProperties.PREFIX)
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
    public static final String PREFIX = "sixpay.customer.verification.banking";
    public static final String DEFAULT_ENDPOINT_PATH = "/v1/accounts/verify";

    public BankingVerificationProperties {
        if (endpointPath != null) endpointPath = endpointPath.strip();
    }

    @AssertTrue(message = "Banking verification configuration must be secure, positive and consistent")
    public boolean isValid() {
        return baseUrl != null
                && "https".equalsIgnoreCase(baseUrl.getScheme())
                && baseUrl.getHost() != null
                && endpointPath != null
                && endpointPath.startsWith("/")
                && positive(connectTimeout)
                && positive(readTimeout)
                && readTimeout.compareTo(connectTimeout) >= 0
                && positive(retryBackoff)
                && positive(evidenceTtl)
                && security != null
                && security.isValid();
    }

    private static boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }

    public record Security(
            @NotBlank String oauth2RegistrationId,
            @NotBlank String sslBundle
    ) {
        public Security {
            if (oauth2RegistrationId != null) oauth2RegistrationId = oauth2RegistrationId.strip();
            if (sslBundle != null) sslBundle = sslBundle.strip();
        }
        public boolean isValid() {
            return oauth2RegistrationId != null && !oauth2RegistrationId.isBlank()
                    && sslBundle != null && !sslBundle.isBlank();
        }
    }
}

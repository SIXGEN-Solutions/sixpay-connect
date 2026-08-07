package com.sixpay.accounting.infrastructure.accountingapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(
        prefix = AccountingApiProperties.PREFIX
)
public record AccountingApiProperties(
        boolean enabled,
        URI baseUrl,
        String submitPath,
        String batchLookupPath,
        String idempotencyLookupPath,
        Duration connectTimeout,
        Duration readTimeout,
        Security security,
        Contract contract
) {
    public static final String PREFIX =
            "sixpay.accounting.api";

    public AccountingApiProperties {
        validateBaseUrl(baseUrl);
        submitPath = path(
                submitPath,
                "submitPath"
        );
        batchLookupPath = path(
                batchLookupPath,
                "batchLookupPath"
        );
        idempotencyLookupPath = path(
                idempotencyLookupPath,
                "idempotencyLookupPath"
        );
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
        security = Objects.requireNonNull(
                security,
                "security"
        );
        contract = Objects.requireNonNull(
                contract,
                "contract"
        );
    }

    public record Security(
            String oauth2RegistrationId,
            String sslBundle
    ) {
        public Security {
            oauth2RegistrationId = required(
                    oauth2RegistrationId,
                    "oauth2RegistrationId"
            );
            sslBundle = required(
                    sslBundle,
                    "sslBundle"
            );
        }
    }

    public record Contract(
            String idempotencyHeader
    ) {
        public Contract {
            idempotencyHeader = required(
                    idempotencyHeader,
                    "idempotencyHeader"
            );
        }
    }

    private static void validateBaseUrl(URI value) {
        if (value == null
                || !value.isAbsolute()
                || value.getHost() == null
                || !"https".equalsIgnoreCase(
                        value.getScheme()
                )) {
            throw new IllegalArgumentException(
                    "baseUrl must be an absolute HTTPS URI"
            );
        }
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

    private static String path(
            String value,
            String name
    ) {
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
}

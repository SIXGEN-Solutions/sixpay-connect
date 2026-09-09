package com.sixpay.accounting.infrastructure.tresorpay.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = TresorPayStatusProperties.PREFIX)
public record TresorPayStatusProperties(
        boolean enabled,
        URI baseUrl,
        String statusPath,
        Duration connectTimeout,
        Duration readTimeout,
        Security security
) {
    public static final String PREFIX =
            "sixpay.accounting.tresorpay-status";

    public TresorPayStatusProperties {
        if (baseUrl == null
                || !baseUrl.isAbsolute()
                || baseUrl.getHost() == null
                || !"https".equalsIgnoreCase(baseUrl.getScheme())) {
            throw new IllegalArgumentException(
                    "baseUrl must be an absolute HTTPS URI"
            );
        }
        if (statusPath == null
                || statusPath.isBlank()
                || !statusPath.startsWith("/")) {
            throw new IllegalArgumentException(
                    "statusPath must start with /"
            );
        }
        if (connectTimeout == null
                || connectTimeout.isZero()
                || connectTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "connectTimeout must be positive"
            );
        }
        if (readTimeout == null
                || readTimeout.isZero()
                || readTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "readTimeout must be positive"
            );
        }
        if (readTimeout.compareTo(connectTimeout) < 0) {
            throw new IllegalArgumentException(
                    "readTimeout must be >= connectTimeout"
            );
        }
        if (security == null) {
            throw new IllegalArgumentException(
                    "security is required"
            );
        }
    }

    public record Security(
            String oauth2RegistrationId,
            String sslBundle
    ) {
        public Security {
            if (oauth2RegistrationId == null
                    || oauth2RegistrationId.isBlank()) {
                throw new IllegalArgumentException(
                        "oauth2RegistrationId is required"
                );
            }
            if (sslBundle == null || sslBundle.isBlank()) {
                throw new IllegalArgumentException(
                        "sslBundle is required"
                );
            }
        }
    }
}

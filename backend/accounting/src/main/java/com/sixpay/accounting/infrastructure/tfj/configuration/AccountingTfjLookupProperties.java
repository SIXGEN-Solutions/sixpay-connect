package com.sixpay.accounting.infrastructure.tfj.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = AccountingTfjLookupProperties.PREFIX)
public record AccountingTfjLookupProperties(
        boolean enabled,
        URI baseUrl,
        String lookupPath,
        Duration connectTimeout,
        Duration readTimeout,
        Security security
) {
    public static final String PREFIX = "sixpay.accounting.tfj.lookup";

    public AccountingTfjLookupProperties {
        if (baseUrl == null || !baseUrl.isAbsolute() || baseUrl.getHost() == null
                || !("https".equalsIgnoreCase(baseUrl.getScheme())
                    || (security != null && security.allowInsecureHttp() && !security.mtlsEnabled()
                    && "http".equalsIgnoreCase(baseUrl.getScheme())
                    && ("localhost".equalsIgnoreCase(baseUrl.getHost())
                        || "127.0.0.1".equals(baseUrl.getHost())
                        || "::1".equals(baseUrl.getHost()))))) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTPS URI");
        }
        if (lookupPath == null || lookupPath.isBlank() || !lookupPath.startsWith("/")) {
            throw new IllegalArgumentException("lookupPath must start with /");
        }
        connectTimeout = positive(connectTimeout, "connectTimeout");
        readTimeout = positive(readTimeout, "readTimeout");
        if (readTimeout.compareTo(connectTimeout) < 0) {
            throw new IllegalArgumentException("readTimeout must be >= connectTimeout");
        }
        security = Objects.requireNonNull(security, "security");
    }

    public record Security(boolean mtlsEnabled, boolean allowInsecureHttp, String sslBundle) {
        public Security {
            if (mtlsEnabled && (sslBundle == null || sslBundle.isBlank())) {
                throw new IllegalArgumentException("sslBundle is required when mTLS is enabled");
            }
            if (mtlsEnabled && allowInsecureHttp) {
                throw new IllegalArgumentException("insecure HTTP cannot be combined with mTLS");
            }
        }
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}

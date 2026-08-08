package com.sixpay.reporting.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ConfigurationProperties(
        prefix = "sixpay.reporting.audit-query"
)
public record ReportingAuditQueryProperties(
        String cursorHmacKey
) {
    public byte[] decodedKey() {
        if (cursorHmacKey == null || cursorHmacKey.isBlank()) {
            throw new IllegalStateException(
                    "sixpay.reporting.audit-query.cursor-hmac-key is required"
            );
        }
        try {
            byte[] decoded = Base64.getDecoder()
                    .decode(cursorHmacKey);
            if (decoded.length < 32) {
                throw new IllegalStateException(
                        "audit cursor HMAC key must decode to at least 32 bytes"
                );
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "audit cursor HMAC key must be valid Base64",
                    exception
            );
        }
    }
}

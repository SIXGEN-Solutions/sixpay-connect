package com.sixpay.security.configuration;

import com.sixpay.security.domain.authentication.PasswordPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sixpay.security.local.password")
public record PasswordPolicyProperties(
        Integer minLength,
        Integer maxLength,
        Integer historySize,
        Integer expirationDays
) {

    static final int DEFAULT_MIN_LENGTH = 12;
    static final int DEFAULT_MAX_LENGTH = 200;
    static final int DEFAULT_HISTORY_SIZE = 5;
    static final int DEFAULT_EXPIRATION_DAYS = 90;

    public PasswordPolicyProperties {
        minLength = minLength == null
                ? DEFAULT_MIN_LENGTH
                : minLength;
        maxLength = maxLength == null
                ? DEFAULT_MAX_LENGTH
                : maxLength;
        historySize = historySize == null
                ? DEFAULT_HISTORY_SIZE
                : historySize;
        expirationDays = expirationDays == null
                ? DEFAULT_EXPIRATION_DAYS
                : expirationDays;

        // Reuse domain invariants so invalid external configuration fails fast.
        new PasswordPolicy(
                minLength,
                maxLength,
                historySize,
                expirationDays
        );
    }

    public PasswordPolicy toDomain() {
        return new PasswordPolicy(
                minLength,
                maxLength,
                historySize,
                expirationDays
        );
    }
}

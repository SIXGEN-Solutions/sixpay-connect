package com.sixpay.customer.observation.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(
        prefix = "sixpay.customer.observation.persistence"
)
public record ObservedCustomerPersistenceProperties(
        boolean enabled,
        String protectionKeyBase64,
        int maxOptimisticAttempts
) {

    private static final int MIN_KEY_BASE64_LENGTH = 44;

    public ObservedCustomerPersistenceProperties {
        if (maxOptimisticAttempts < 1
                || maxOptimisticAttempts > 10) {
            throw new IllegalArgumentException(
                    "maxOptimisticAttempts must be between 1 and 10"
            );
        }

        if (enabled) {
            protectionKeyBase64 = Objects.requireNonNull(
                    protectionKeyBase64,
                    "protectionKeyBase64 is required when "
                            + "Observed Customer persistence is enabled"
            ).strip();

            if (protectionKeyBase64.length()
                    < MIN_KEY_BASE64_LENGTH) {
                throw new IllegalArgumentException(
                        "protectionKeyBase64 must contain at least "
                                + "32 bytes encoded in Base64"
                );
            }
        }
    }
}

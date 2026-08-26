package com.sixpay.customer.observation.configuration;

import org.springframework.boot.context.properties
        .ConfigurationProperties;
import org.springframework.boot.context.properties.bind
        .DefaultValue;

@ConfigurationProperties(
        prefix = "sixpay.customer.observation.persistence"
)
public record ObservedCustomerPersistenceProperties(
        @DefaultValue("true")
        boolean enabled,

        @Deprecated
        @DefaultValue("3")
        int maxOptimisticAttempts,

        String protectionKeyBase64
) {

    public ObservedCustomerPersistenceProperties {
        if (maxOptimisticAttempts < 1
                || maxOptimisticAttempts > 10) {
            throw new IllegalArgumentException(
                    "maxOptimisticAttempts must be between 1 and 10"
            );
        }

        if (protectionKeyBase64 == null
                || protectionKeyBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "protectionKeyBase64 must not be blank"
            );
        }

        protectionKeyBase64 =
                protectionKeyBase64.strip();
    }
}
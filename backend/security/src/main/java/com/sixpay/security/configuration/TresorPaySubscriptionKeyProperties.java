package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sixpay.security.tresorpay.subscription-key")
public record TresorPaySubscriptionKeyProperties(
        boolean enabled,
        String value
) {

    public TresorPaySubscriptionKeyProperties {
        if (enabled && (value == null || value.isBlank())) {
            throw new IllegalArgumentException(
                    "TRESOR PAY subscription key must not be blank when enabled"
            );
        }
        if (value != null) {
            value = value.trim();
        }
    }
}

package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sixpay.security.partner.subscription-key")
public record PartnerSubscriptionKeyProperties(
        boolean enabled,
        String value
) {

    public PartnerSubscriptionKeyProperties {
        if (enabled && (value == null || value.isBlank())) {
            throw new IllegalArgumentException(
                    "Partner subscription key must not be blank when enabled"
            );
        }
        if (value != null) {
            value = value.trim();
        }
    }
}

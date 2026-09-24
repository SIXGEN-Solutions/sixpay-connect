package com.sixpay.accounting.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "sixpay.accounting.external-status-verification")
public record AccountingExternalStatusVerificationProperties(
        boolean enabled,
        Map<String, Boolean> partners
) {
    public AccountingExternalStatusVerificationProperties {
        partners = partners == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(partners));
    }

    public boolean appliesTo(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        return partners.getOrDefault(partnerId.strip(), enabled);
    }
}

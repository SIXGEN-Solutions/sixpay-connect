package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/** Keyed by financial institution and claim type (AVI/IM7/RNF). */
@ConfigurationProperties(prefix = "sixpay.payment.treasury")
public record TreasuryResolutionProperties(
        boolean enabled,
        Map<String, Map<String, Account>> accounts
) {
    public TreasuryResolutionProperties {
        accounts = accounts == null ? Map.of() : Map.copyOf(accounts);
    }

    public record Account(
            String configurationId,
            String accountReference,
            String version
    ) { }
}

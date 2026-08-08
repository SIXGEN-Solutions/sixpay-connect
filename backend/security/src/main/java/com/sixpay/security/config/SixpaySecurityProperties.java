package com.sixpay.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sixpay.security")
public record SixpaySecurityProperties(
        SixpayAuthenticationMode authenticationMode,
        Local local
) {
    public SixpaySecurityProperties {
        authenticationMode = authenticationMode == null
                ? SixpayAuthenticationMode.OIDC
                : authenticationMode;
        local = local == null ? new Local(false) : local;
    }

    public record Local(boolean seedEnabled) {
    }
}

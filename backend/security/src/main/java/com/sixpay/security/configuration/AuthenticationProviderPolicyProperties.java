package com.sixpay.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sixpay.security.authentication.policy")
public record AuthenticationProviderPolicyProperties(
        boolean requireProvider
) {
}

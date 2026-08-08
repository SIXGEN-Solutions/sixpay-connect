package com.sixpay.security.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sixpay.security.local.seed")
public record LocalAuthSeedProperties(
        String adminPassword,
        String managerPassword,
        String auditorPassword,
        String partnerPassword,
        String partnerSubject
) {
}

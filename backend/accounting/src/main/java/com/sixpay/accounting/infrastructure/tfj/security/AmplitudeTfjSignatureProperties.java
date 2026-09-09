package com.sixpay.accounting.infrastructure.tfj.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "sixpay.accounting.tfj.webhook")
public record AmplitudeTfjSignatureProperties(
        boolean enabled,
        Map<String, String> signatureKeysBase64
) {
    public AmplitudeTfjSignatureProperties {
        signatureKeysBase64 = signatureKeysBase64 == null
                ? Map.of()
                : Map.copyOf(signatureKeysBase64);
    }
}

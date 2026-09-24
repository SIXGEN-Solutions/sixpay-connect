package com.sixpay.payment.application.port.output.partner;

import java.util.Objects;

public record PartnerExtensionDefinition(
        String key,
        PartnerExtensionValueType valueType,
        boolean idempotencySignificant,
        boolean queryExposable,
        boolean callbackExposable,
        String securityClassification
) {
    public PartnerExtensionDefinition {
        key = Objects.requireNonNull(key, "Extension key").trim();
        if (!key.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$")) {
            throw new IllegalArgumentException("Invalid Partner extension key");
        }
        valueType = Objects.requireNonNull(valueType, "Extension value type");
        securityClassification = securityClassification == null
                ? null : securityClassification.trim();
    }
}

package com.sixpay.partner.application.contract;

import java.util.List;
import java.util.Objects;

public record PartnerExtensionProfileView(
        List<PartnerExtensionDefinitionView> definitions
) {
    public PartnerExtensionProfileView {
        definitions = List.copyOf(Objects.requireNonNull(definitions));
        if (definitions.size() > 20) {
            throw new IllegalArgumentException(
                    "Partner extension profile exceeds 20 definitions"
            );
        }
    }

    public static PartnerExtensionProfileView empty() {
        return new PartnerExtensionProfileView(List.of());
    }

    public record PartnerExtensionDefinitionView(
            String key,
            String valueType,
            boolean idempotencySignificant,
            boolean queryExposable,
            boolean callbackExposable,
            String securityClassification
    ) {}
}

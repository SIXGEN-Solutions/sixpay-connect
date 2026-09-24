package com.sixpay.payment.application.port.output.partner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record PartnerExtensionProfile(
        Map<String, PartnerExtensionDefinition> definitions
) {
    public static final int MAX_EXTENSIONS = 20;

    public PartnerExtensionProfile {
        Objects.requireNonNull(definitions, "Partner extension definitions");
        if (definitions.size() > MAX_EXTENSIONS) {
            throw new IllegalArgumentException(
                    "Partner extension profile exceeds " + MAX_EXTENSIONS + " definitions"
            );
        }
        var copy = new LinkedHashMap<String, PartnerExtensionDefinition>();
        definitions.forEach((key, definition) -> {
            Objects.requireNonNull(definition, "Partner extension definition");
            if (!key.equals(definition.key())) {
                throw new IllegalArgumentException(
                        "Partner extension definition key mismatch: " + key
                );
            }
            copy.put(key, definition);
        });
        definitions = Map.copyOf(copy);
    }

    public static PartnerExtensionProfile empty() {
        return new PartnerExtensionProfile(Map.of());
    }
}

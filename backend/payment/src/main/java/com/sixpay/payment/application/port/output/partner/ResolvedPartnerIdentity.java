package com.sixpay.payment.application.port.output.partner;

import java.util.Objects;
import java.util.UUID;

/**
 * Minimal Payment-owned representation of a resolved Partner identity.
 */
public record ResolvedPartnerIdentity(
        UUID partnerId,
        String partnerIdentifier,
        boolean acceptsNewTransactions
) {

    public ResolvedPartnerIdentity {
        partnerId = Objects.requireNonNull(
                partnerId,
                "partnerId is required"
        );
        partnerIdentifier = requireText(
                partnerIdentifier,
                "partnerIdentifier"
        );
    }

    private static String requireText(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}

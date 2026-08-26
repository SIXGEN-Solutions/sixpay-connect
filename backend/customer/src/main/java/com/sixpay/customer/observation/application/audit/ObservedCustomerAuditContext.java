package com.sixpay.customer.observation.application.audit;

import java.util.Objects;

public record ObservedCustomerAuditContext(
        String actorId,
        String correlationId
) {
    public static final int MAX_ACTOR_ID_LENGTH = 150;
    public static final int MAX_CORRELATION_ID_LENGTH = 150;

    public ObservedCustomerAuditContext {
        actorId = requireText(actorId, MAX_ACTOR_ID_LENGTH, "actorId");
        correlationId = requireText(
                correlationId,
                MAX_CORRELATION_ID_LENGTH,
                "correlationId"
        );
    }

    public static ObservedCustomerAuditContext system(
            String correlationId
    ) {
        return new ObservedCustomerAuditContext(
                "sixpay-system",
                correlationId
        );
    }

    private static String requireText(
            String value,
            int maxLength,
            String label
    ) {
        Objects.requireNonNull(value, label + " is required");
        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "ObservedCustomerAuditContext[PROTECTED]";
    }
}

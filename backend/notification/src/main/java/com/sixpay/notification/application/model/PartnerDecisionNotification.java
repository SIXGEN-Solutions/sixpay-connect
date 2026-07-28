package com.sixpay.notification.application.model;

import java.util.Objects;
import java.util.UUID;

public record PartnerDecisionNotification(
        UUID eventId,
        UUID partnerId,
        String recipientEmail,
        Decision decision,
        String reason,
        String correlationId
) {

    public PartnerDecisionNotification {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        recipientEmail = requireText(recipientEmail, "recipientEmail");
        Objects.requireNonNull(decision, "decision is required");
        if (decision == Decision.REJECTED) {
            reason = requireText(reason, "reason");
        } else if (reason != null && reason.isBlank()) {
            reason = null;
        }
        correlationId = requireText(correlationId, "correlationId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    public enum Decision {
        APPROVED,
        REJECTED
    }
}

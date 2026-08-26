package com.sixpay.notification.application.model;

import java.util.UUID;
import java.time.Instant;

public record PartnerStatusChangedEvent(
        int schemaVersion,
        UUID eventId,
        UUID partnerId,
        String previousStatus,
        String currentStatus,
        String reason,
        String recipientEmail,
        String actorId,
        String correlationId,
        Instant occurredAt
) {
}

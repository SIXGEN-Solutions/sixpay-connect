package com.sixpay.reporting.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentTimelineEntryResponse(
        UUID timelineEntryId,
        UUID paymentId,
        String category,
        String eventType,
        String fromState,
        String toState,
        String result,
        String reasonCode,
        Instant occurredAt,
        UUID correlationId,
        String sourceSystem,
        String externalReference,
        long aggregateVersion,
        Map<String, Object> metadata
) {
}

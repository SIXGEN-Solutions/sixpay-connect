package com.sixpay.payment.infrastructure.outbox.claim;

import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentOutboxClaim(
        UUID eventId,
        UUID aggregateId,
        String aggregateType,
        String eventType,
        int schemaVersion,
        String correlationId,
        String payload,
        Instant occurredAt,
        int attempt,
        Instant claimedAt,
        String claimedBy
) {

    public PaymentOutboxClaim {
        eventId = Objects.requireNonNull(eventId);
        aggregateId = Objects.requireNonNull(aggregateId);
        aggregateType = requireText(aggregateType, "aggregateType");
        eventType = requireText(eventType, "eventType");

        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "schemaVersion must be at least one"
            );
        }

        correlationId = requireText(
                correlationId,
                "correlationId"
        );
        payload = requireText(payload, "payload");
        occurredAt = Objects.requireNonNull(occurredAt);

        if (attempt < 1) {
            throw new IllegalArgumentException(
                    "attempt must be at least one"
            );
        }

        claimedAt = Objects.requireNonNull(claimedAt);
        claimedBy = requireText(claimedBy, "claimedBy");
    }

    public static PaymentOutboxClaim from(
            PaymentOutboxEntity source
    ) {
        Objects.requireNonNull(source, "source is required");

        if (source.status()
                != PaymentOutboxEntity.Status.PROCESSING) {
            throw new IllegalArgumentException(
                    "Only a processing outbox event can "
                            + "be exposed as a claim"
            );
        }

        return new PaymentOutboxClaim(
                source.eventId(),
                source.aggregateId(),
                source.aggregateType(),
                source.eventType(),
                source.schemaVersion(),
                source.correlationId(),
                source.payload(),
                source.occurredAt(),
                source.attemptCount(),
                source.claimedAt(),
                source.claimedBy()
        );
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return "PaymentOutboxClaim["
                + "eventId=" + eventId
                + ", aggregateId=" + aggregateId
                + ", aggregateType=" + aggregateType
                + ", eventType=" + eventType
                + ", schemaVersion=" + schemaVersion
                + ", correlationId=" + correlationId
                + ", payload=[PROTECTED]"
                + ", occurredAt=" + occurredAt
                + ", attempt=" + attempt
                + ", claimedAt=" + claimedAt
                + ", claimedBy=" + claimedBy
                + "]";
    }
}

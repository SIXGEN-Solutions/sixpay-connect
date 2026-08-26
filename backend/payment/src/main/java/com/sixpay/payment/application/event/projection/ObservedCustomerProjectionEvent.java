package com.sixpay.payment.application.event.projection;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable, versioned Payment-owned event contract consumed by Observed Customer.
 */
public record ObservedCustomerProjectionEvent(
        UUID eventId,
        int eventVersion,
        UUID paymentId,
        String aggregateType,
        long aggregateVersion,
        ObservedCustomerProjectionEventType eventType,
        ObservedCustomerProjectionPayload payload,
        String correlationId,
        Instant occurredAt
) {
    public static final int CURRENT_EVENT_VERSION = 1;
    public static final String PAYMENT_AGGREGATE_TYPE = "PAYMENT";
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerProjectionEvent {
        eventId = requireUuid(eventId, "eventId");
        paymentId = requireUuid(paymentId, "paymentId");
        if (eventVersion != CURRENT_EVENT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported observed-customer projection event version: "
                            + eventVersion
            );
        }
        aggregateType = requireText(aggregateType, "aggregateType")
                .toUpperCase(Locale.ROOT);
        if (!PAYMENT_AGGREGATE_TYPE.equals(aggregateType)) {
            throw new IllegalArgumentException(
                    "aggregateType must be PAYMENT"
            );
        }
        if (aggregateVersion < 1) {
            throw new IllegalArgumentException(
                    "aggregateVersion must be at least one"
            );
        }
        eventType = Objects.requireNonNull(
                eventType,
                "eventType is required"
        );
        payload = Objects.requireNonNull(payload, "payload is required");
        correlationId = requireCanonicalUuidText(
                correlationId,
                "correlationId"
        );
        occurredAt = Objects.requireNonNull(
                occurredAt,
                "occurredAt is required"
        );
        if (payload.paymentUpdatedAt().isAfter(occurredAt)) {
            throw new IllegalArgumentException(
                    "payload paymentUpdatedAt must not be after event occurredAt"
            );
        }
    }

    public static ObservedCustomerProjectionEvent versionOne(
            UUID eventId,
            UUID paymentId,
            long aggregateVersion,
            ObservedCustomerProjectionEventType eventType,
            ObservedCustomerProjectionPayload payload,
            String correlationId,
            Instant occurredAt
    ) {
        return new ObservedCustomerProjectionEvent(
                eventId,
                CURRENT_EVENT_VERSION,
                paymentId,
                PAYMENT_AGGREGATE_TYPE,
                aggregateVersion,
                eventType,
                payload,
                correlationId,
                occurredAt
        );
    }

    private static UUID requireUuid(UUID value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException(field + " must not be nil");
        }
        return value;
    }

    private static String requireCanonicalUuidText(
            String value,
            String field
    ) {
        String normalized = requireText(value, field);
        try {
            UUID parsed = UUID.fromString(normalized);
            if (!parsed.toString().equals(normalized)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    field + " must be a canonical UUID",
                    exception
            );
        }
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return "ObservedCustomerProjectionEvent["
                + "eventId=" + eventId
                + ", eventVersion=" + eventVersion
                + ", paymentId=" + paymentId
                + ", aggregateType=" + aggregateType
                + ", aggregateVersion=" + aggregateVersion
                + ", eventType=" + eventType
                + ", payload=[PROTECTED]"
                + ", correlationId=" + correlationId
                + ", occurredAt=" + occurredAt
                + "]";
    }
}

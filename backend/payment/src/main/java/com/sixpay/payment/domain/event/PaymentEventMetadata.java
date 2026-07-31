package com.sixpay.payment.domain.event;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Common immutable metadata shared by every Payment domain event.
 */
public record PaymentEventMetadata(
        UUID eventId,
        PaymentId paymentId,
        PublicPaymentReference paymentReference,
        CorrelationId correlationId,
        PaymentStatus paymentStatus,
        long aggregateVersion,
        int eventSequence,
        UUID causationId,
        Instant occurredAt
) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public PaymentEventMetadata {
        eventId = Objects.requireNonNull(eventId, "Event ID");
        if (NIL_UUID.equals(eventId) || eventId.version() != 4) {
            throw new IllegalArgumentException(
                    "Event ID must be a non-nil UUID v4"
            );
        }
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Public Payment reference"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "Payment status"
        );
        if (aggregateVersion <= 0) {
            throw new IllegalArgumentException(
                    "Aggregate version must be positive"
            );
        }
        if (eventSequence <= 0) {
            throw new IllegalArgumentException(
                    "Event sequence must be positive"
            );
        }
        if (causationId != null && NIL_UUID.equals(causationId)) {
            throw new IllegalArgumentException(
                    "Causation ID must not be the nil UUID"
            );
        }
        occurredAt = Objects.requireNonNull(
                occurredAt,
                "Event occurrence instant"
        );
    }

    public Optional<UUID> causationIdOptional() {
        return Optional.ofNullable(causationId);
    }
}

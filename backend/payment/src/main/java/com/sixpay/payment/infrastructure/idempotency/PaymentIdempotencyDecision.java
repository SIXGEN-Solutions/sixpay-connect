package com.sixpay.payment.infrastructure.idempotency;

import java.util.Objects;
import java.util.UUID;

/**
 * Result returned when an idempotency key is inspected or reserved.
 */
public record PaymentIdempotencyDecision(
        Kind kind,
        UUID paymentId,
        String responseStatus,
        String responsePayload
) {

    public enum Kind {
        NEW,
        IN_PROGRESS,
        REPLAY
    }

    public PaymentIdempotencyDecision {
        kind = Objects.requireNonNull(
                kind,
                "Idempotency decision kind"
        );

        if (kind == Kind.REPLAY) {
            Objects.requireNonNull(
                    paymentId,
                    "Replay Payment ID"
            );
            if (responseStatus == null
                    || responseStatus.isBlank()
                    || responsePayload == null
                    || responsePayload.isBlank()) {
                throw new IllegalArgumentException(
                        "Replay requires a persisted response"
                );
            }
        } else if (paymentId != null
                || responseStatus != null
                || responsePayload != null) {
            throw new IllegalArgumentException(
                    "Only a replay decision may carry a response"
            );
        }
    }

    static PaymentIdempotencyDecision newRequest() {
        return new PaymentIdempotencyDecision(
                Kind.NEW,
                null,
                null,
                null
        );
    }

    static PaymentIdempotencyDecision inProgress() {
        return new PaymentIdempotencyDecision(
                Kind.IN_PROGRESS,
                null,
                null,
                null
        );
    }

    static PaymentIdempotencyDecision replay(
            PaymentIdempotencyEntity entity
    ) {
        return new PaymentIdempotencyDecision(
                Kind.REPLAY,
                entity.paymentId(),
                entity.responseStatus(),
                entity.responsePayload()
        );
    }
}

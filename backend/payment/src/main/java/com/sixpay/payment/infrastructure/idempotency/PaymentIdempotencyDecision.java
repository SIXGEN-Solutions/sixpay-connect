package com.sixpay.payment.infrastructure.idempotency;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Result returned when an idempotency key is inspected or reserved.
 *
 * <p>OUTCOME_UNKNOWN is a technical integration state only. Callers must
 * perform authoritative recovery before deciding whether retry is legal.</p>
 */
public record PaymentIdempotencyDecision(
        Kind kind,
        UUID paymentId,
        String responseStatus,
        String responsePayload,
        String recoveryReference,
        String recoveryReason,
        Instant unknownOutcomeAt
) {

    public enum Kind {
        NEW,
        IN_PROGRESS,
        OUTCOME_UNKNOWN,
        REPLAY
    }

    public PaymentIdempotencyDecision {
        kind = Objects.requireNonNull(
                kind,
                "Idempotency decision kind"
        );

        switch (kind) {
            case REPLAY -> {
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
                if (recoveryReference != null
                        || recoveryReason != null
                        || unknownOutcomeAt != null) {
                    throw new IllegalArgumentException(
                            "Replay must not carry recovery metadata"
                    );
                }
            }
            case OUTCOME_UNKNOWN -> {
                Objects.requireNonNull(
                        paymentId,
                        "Unknown-outcome Payment ID"
                );
                Objects.requireNonNull(
                        unknownOutcomeAt,
                        "Unknown-outcome instant"
                );
                if (recoveryReason == null
                        || recoveryReason.isBlank()) {
                    throw new IllegalArgumentException(
                            "Unknown outcome requires a recovery reason"
                    );
                }
                if (responseStatus != null
                        || responsePayload != null) {
                    throw new IllegalArgumentException(
                            "Unknown outcome must not carry a replay response"
                    );
                }
            }
            case NEW, IN_PROGRESS -> {
                if (paymentId != null
                        || responseStatus != null
                        || responsePayload != null
                        || recoveryReference != null
                        || recoveryReason != null
                        || unknownOutcomeAt != null) {
                    throw new IllegalArgumentException(
                            "New/in-progress decision must not carry result metadata"
                    );
                }
            }
        }
    }

    static PaymentIdempotencyDecision newRequest() {
        return new PaymentIdempotencyDecision(
                Kind.NEW,
                null,
                null,
                null,
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
                null,
                null,
                null,
                null
        );
    }

    static PaymentIdempotencyDecision outcomeUnknown(
            PaymentIdempotencyEntity entity
    ) {
        return new PaymentIdempotencyDecision(
                Kind.OUTCOME_UNKNOWN,
                entity.paymentId(),
                null,
                null,
                entity.recoveryReference(),
                entity.recoveryReason(),
                entity.unknownOutcomeAt()
        );
    }

    static PaymentIdempotencyDecision replay(
            PaymentIdempotencyEntity entity
    ) {
        return new PaymentIdempotencyDecision(
                Kind.REPLAY,
                entity.paymentId(),
                entity.responseStatus(),
                entity.responsePayload(),
                null,
                null,
                null
        );
    }
}

package com.sixpay.bootstrap.integration.customer.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounded operational result for one Payment outbox attempt.
 */
public record PaymentObservedCustomerOutboxResult(
        UUID eventId,
        Outcome outcome,
        int attempt,
        String errorType,
        Instant nextAttemptAt
) {

    public PaymentObservedCustomerOutboxResult {
        eventId = Objects.requireNonNull(eventId, "eventId is required");
        outcome = Objects.requireNonNull(outcome, "outcome is required");

        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be at least one");
        }

        if (outcome == Outcome.RETRY_SCHEDULED && nextAttemptAt == null) {
            throw new IllegalArgumentException(
                    "nextAttemptAt is required for retry"
            );
        }

        if (outcome != Outcome.RETRY_SCHEDULED && nextAttemptAt != null) {
            throw new IllegalArgumentException(
                    "nextAttemptAt is only allowed for retry"
            );
        }
    }

    public enum Outcome {
        PUBLISHED,
        RETRY_SCHEDULED,
        DEAD_LETTERED
    }
}

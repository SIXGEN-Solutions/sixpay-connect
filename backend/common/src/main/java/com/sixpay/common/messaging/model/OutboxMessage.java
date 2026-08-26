package com.sixpay.common.messaging.model;

import com.sixpay.common.validation.Preconditions;

/**
 * Claimed Outbox message ready for publication.
 *
 * @param event integration event to publish
 * @param attemptCount current publication attempt, starting at one
 */
public record OutboxMessage(
        IntegrationEventEnvelope event,
        int attemptCount
) {

    public OutboxMessage {
        event = Preconditions.requireNonNull(
                event,
                "Integration event must not be null"
        );
        Preconditions.requireTrue(
                attemptCount > 0,
                "Attempt count must be positive"
        );
    }
}

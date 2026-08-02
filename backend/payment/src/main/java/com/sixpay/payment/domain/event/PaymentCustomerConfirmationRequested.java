package com.sixpay.payment.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Customer confirmation has been requested for the Payment.
 */
public record PaymentCustomerConfirmationRequested(
        PaymentEventMetadata metadata,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentCustomerConfirmationRequested {
        metadata = Objects.requireNonNull(
                metadata,
                "Event metadata"
        );
        requestedAt = Objects.requireNonNull(
                requestedAt,
                "Requested instant"
        );
    }
}

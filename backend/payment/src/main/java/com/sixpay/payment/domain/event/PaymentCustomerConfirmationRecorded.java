package com.sixpay.payment.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Customer confirmation has been accepted for the Payment.
 */
public record PaymentCustomerConfirmationRecorded(
        PaymentEventMetadata metadata,
        Instant confirmedAt
) implements PaymentDomainEvent {

    public PaymentCustomerConfirmationRecorded {
        metadata = Objects.requireNonNull(
                metadata,
                "Event metadata"
        );
        confirmedAt = Objects.requireNonNull(
                confirmedAt,
                "Confirmed instant"
        );
    }
}

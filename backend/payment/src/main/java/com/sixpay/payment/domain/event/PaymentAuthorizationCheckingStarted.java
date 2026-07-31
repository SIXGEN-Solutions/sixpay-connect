package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested authorization-evidence evaluation.
 */
public record PaymentAuthorizationCheckingStarted(
        PaymentEventMetadata metadata,
        Instant startedAt
) implements PaymentDomainEvent {

    public PaymentAuthorizationCheckingStarted {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }
}

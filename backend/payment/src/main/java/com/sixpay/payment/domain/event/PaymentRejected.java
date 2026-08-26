package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has reached terminal REJECTED with proven absence of financial effect.
 */
public record PaymentRejected(
        PaymentEventMetadata metadata,
        FailureCode failureCode,
        FailureCategory failureCategory,
        FailureStage failureStage,
        Instant finalizedAt
) implements PaymentDomainEvent {

    public PaymentRejected {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        failureCode = Objects.requireNonNull(failureCode, "failureCode");
        failureCategory = Objects.requireNonNull(failureCategory, "failureCategory");
        failureStage = Objects.requireNonNull(failureStage, "failureStage");
        finalizedAt = Objects.requireNonNull(finalizedAt, "finalizedAt");
    }
}

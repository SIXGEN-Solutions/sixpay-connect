package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment processing remains non-terminal while controlled recovery or operator action is required.
 */
public record PaymentProcessingDeferred(
        PaymentEventMetadata metadata,
        FailureCode failureCode,
        FailureCategory failureCategory,
        FailureStage failureStage,
        RetryDisposition retryDisposition,
        Instant deferredAt
) implements PaymentDomainEvent {

    public PaymentProcessingDeferred {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        failureCode = Objects.requireNonNull(failureCode, "failureCode");
        failureCategory = Objects.requireNonNull(failureCategory, "failureCategory");
        failureStage = Objects.requireNonNull(failureStage, "failureStage");
        retryDisposition = Objects.requireNonNull(retryDisposition, "retryDisposition");
        deferredAt = Objects.requireNonNull(deferredAt, "deferredAt");
    }
}

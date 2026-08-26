package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A conclusive reversal result is durably available for delivery.
 */
public record PaymentReversalResultAvailable(
        PaymentEventMetadata metadata,
        ExternalPaymentReference externalPaymentReference,
        PaymentReversalResultType resultType,
        String originalPostingReference,
        ReversalReference reversalReference,
        FailureCode failureCode,
        Instant availableAt
) implements PaymentDomainEvent {

    public PaymentReversalResultAvailable {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        externalPaymentReference = Objects.requireNonNull(externalPaymentReference, "externalPaymentReference");
        resultType = Objects.requireNonNull(resultType, "resultType");
        originalPostingReference = Objects.requireNonNull(originalPostingReference, "originalPostingReference");
        originalPostingReference = originalPostingReference.strip();
        if (originalPostingReference.isEmpty() || originalPostingReference.length() > 256) {
            throw new IllegalArgumentException("originalPostingReference has an invalid length");
        }
        availableAt = Objects.requireNonNull(availableAt, "availableAt");
        if (resultType == PaymentReversalResultType.REVERSED
                && reversalReference == null) {
            throw new IllegalArgumentException(
                    "Reversed result requires a reversal reference"
            );
        }
        if (resultType == PaymentReversalResultType.REVERSAL_REQUIRED
                && failureCode == null) {
            throw new IllegalArgumentException(
                    "Reversal-required result requires a failure code"
            );
        }
    }
}

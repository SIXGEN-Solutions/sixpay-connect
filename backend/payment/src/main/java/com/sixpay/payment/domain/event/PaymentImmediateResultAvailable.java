package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A non-final or immediate Payment result is durably available for delivery.
 */
public record PaymentImmediateResultAvailable(
        PaymentEventMetadata metadata,
        ExternalPaymentReference externalPaymentReference,
        PaymentImmediateResultType resultType,
        FailureCode failureCode,
        String principalPostingReference,
        LocalDate businessDate,
        Instant availableAt
) implements PaymentDomainEvent {

    public PaymentImmediateResultAvailable {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        externalPaymentReference = Objects.requireNonNull(externalPaymentReference, "externalPaymentReference");
        resultType = Objects.requireNonNull(resultType, "resultType");
        if (principalPostingReference != null) {
            principalPostingReference = principalPostingReference.strip();
            if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
                throw new IllegalArgumentException("principalPostingReference has an invalid length");
            }
        }
        availableAt = Objects.requireNonNull(availableAt, "availableAt");
    }
}

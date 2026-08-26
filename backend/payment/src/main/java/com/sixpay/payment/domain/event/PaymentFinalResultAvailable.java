package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The final Treasury-integrated result is durably available for delivery.
 */
public record PaymentFinalResultAvailable(
        PaymentEventMetadata metadata,
        ExternalPaymentReference externalPaymentReference,
        PaymentFinalResultType resultType,
        String principalPostingReference,
        LocalDate businessDate,
        TfjConfirmationId confirmationId,
        Instant availableAt
) implements PaymentDomainEvent {

    public PaymentFinalResultAvailable {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        externalPaymentReference = Objects.requireNonNull(externalPaymentReference, "externalPaymentReference");
        resultType = Objects.requireNonNull(resultType, "resultType");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        availableAt = Objects.requireNonNull(availableAt, "availableAt");
    }
}

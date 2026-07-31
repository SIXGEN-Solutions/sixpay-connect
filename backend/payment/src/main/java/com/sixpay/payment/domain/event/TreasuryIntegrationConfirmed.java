package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Uniquely matched TFJ INTEGRATED evidence has established Treasury finality.
 */
public record TreasuryIntegrationConfirmed(
        PaymentEventMetadata metadata,
        TfjConfirmationId confirmationId,
        String principalPostingReference,
        LocalDate businessDate,
        Instant confirmedAt
) implements PaymentDomainEvent {

    public TreasuryIntegrationConfirmed {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
    }
}

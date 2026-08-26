package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested TFJ finality tracking for a completed posting.
 */
public record PaymentEndOfDayTrackingRequested(
        PaymentEventMetadata metadata,
        FinancialInstitutionCode financialInstitutionCode,
        String principalPostingReference,
        LocalDate businessDate,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentEndOfDayTrackingRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

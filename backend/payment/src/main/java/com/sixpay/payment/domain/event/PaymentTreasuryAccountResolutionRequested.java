package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested protected Treasury-account resolution.
 */
public record PaymentTreasuryAccountResolutionRequested(
        PaymentEventMetadata metadata,
        FinancialInstitutionCode financialInstitutionCode,
        EvidenceFingerprint allocationIntentFingerprint,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentTreasuryAccountResolutionRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        allocationIntentFingerprint = Objects.requireNonNull(allocationIntentFingerprint, "allocationIntentFingerprint");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * All authorization, banking, funds and Treasury evidence permits one posting instruction.
 */
public record PaymentApprovedForPosting(
        PaymentEventMetadata metadata,
        FinancialInstitutionCode financialInstitutionCode,
        MoneyPayload requestedAmount,
        Instant approvedAt
) implements PaymentDomainEvent {

    public PaymentApprovedForPosting {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount");
        approvedAt = Objects.requireNonNull(approvedAt, "approvedAt");
    }
}

package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A canonical authenticated Payment intention has been durably created.
 */
public record PaymentReceived(
        PaymentEventMetadata metadata,
        ExternalPaymentReference externalPaymentReference,
        PaymentSource source,
        FinancialInstitutionCode financialInstitutionCode,
        MoneyPayload requestedAmount,
        String maskedDebtorAccountReference,
        Instant receivedAt
) implements PaymentDomainEvent {

    public PaymentReceived {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        externalPaymentReference = Objects.requireNonNull(externalPaymentReference, "externalPaymentReference");
        source = Objects.requireNonNull(source, "source");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount");
        maskedDebtorAccountReference = Objects.requireNonNull(maskedDebtorAccountReference, "maskedDebtorAccountReference");
        maskedDebtorAccountReference = maskedDebtorAccountReference.strip();
        if (maskedDebtorAccountReference.isEmpty() || maskedDebtorAccountReference.length() > 256) {
            throw new IllegalArgumentException("maskedDebtorAccountReference has an invalid length");
        }
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
    }
}

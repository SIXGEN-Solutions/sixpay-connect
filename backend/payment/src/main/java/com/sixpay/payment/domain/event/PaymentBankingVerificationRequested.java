package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has durably requested fresh banking customer/account verification.
 */
public record PaymentBankingVerificationRequested(
        PaymentEventMetadata metadata,
        FinancialInstitutionCode financialInstitutionCode,
        String debtorAccountBindingFingerprint,
        Instant requestedAt
) implements PaymentDomainEvent {

    public PaymentBankingVerificationRequested {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        financialInstitutionCode = Objects.requireNonNull(financialInstitutionCode, "financialInstitutionCode");
        debtorAccountBindingFingerprint = Objects.requireNonNull(debtorAccountBindingFingerprint, "debtorAccountBindingFingerprint");
        debtorAccountBindingFingerprint = debtorAccountBindingFingerprint.strip();
        if (debtorAccountBindingFingerprint.isEmpty() || debtorAccountBindingFingerprint.length() > 256) {
            throw new IllegalArgumentException("debtorAccountBindingFingerprint has an invalid length");
        }
        if (!debtorAccountBindingFingerprint.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("debtorAccountBindingFingerprint has an invalid format");
        }
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    }
}

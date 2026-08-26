package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public record AccountingPaymentCandidate(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        TresorPayPaymentStatusEvidence tresorPayStatusEvidence
) {
    public AccountingPaymentCandidate {
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        if (paymentId.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException("paymentId must not be nil");
        }
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        paymentBusinessDate = Objects.requireNonNull(paymentBusinessDate, "paymentBusinessDate");
        bankPostingReference = bankPostingReference == null || bankPostingReference.isBlank()
                ? null : bankPostingReference.strip();
        tresorPayStatusEvidence = Objects.requireNonNull(
                tresorPayStatusEvidence,
                "tresorPayStatusEvidence"
        );
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}

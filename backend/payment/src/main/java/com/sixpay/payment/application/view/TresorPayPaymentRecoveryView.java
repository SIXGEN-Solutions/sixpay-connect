package com.sixpay.payment.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TresorPayPaymentRecoveryView(
        UUID paymentId,
        String paymentReference,
        String tresorPayPaymentReference,
        String status,
        Money amount,
        Instant receivedAt,
        Instant updatedAt,
        Instant finalizedAt
) {
    public TresorPayPaymentRecoveryView {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        paymentReference = requireText(paymentReference, "Payment reference");
        tresorPayPaymentReference = requireText(tresorPayPaymentReference, "TRESOR PAY Payment reference");
        status = requireText(status, "Payment status");
        amount = Objects.requireNonNull(amount, "Payment amount");
        receivedAt = Objects.requireNonNull(receivedAt, "Received instant");
        updatedAt = Objects.requireNonNull(updatedAt, "Updated instant");
    }

    public record Money(BigDecimal amount, String currency) {
        public Money {
            amount = Objects.requireNonNull(amount, "Amount");
            currency = requireText(currency, "Currency");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}

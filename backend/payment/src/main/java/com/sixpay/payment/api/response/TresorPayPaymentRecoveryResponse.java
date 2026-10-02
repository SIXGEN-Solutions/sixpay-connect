package com.sixpay.payment.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Public, read-only TRESOR PAY Payment recovery representation.
 *
 * <p>This response intentionally contains only the fields approved by the
 * TRESOR PAY Payment Request contract. It exposes no debtor-account value,
 * internal evidence, OTP material or internal authorization detail.</p>
 */
public record TresorPayPaymentRecoveryResponse(
        UUID paymentId,
        String paymentReference,
        String tresorPayPaymentReference,
        String status,
        Money amount,
        Instant receivedAt,
        Instant updatedAt,
        Instant finalizedAt
) {
    public TresorPayPaymentRecoveryResponse {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        paymentReference = requireText(paymentReference, "Payment reference");
        tresorPayPaymentReference = requireText(
                tresorPayPaymentReference,
                "TRESOR PAY Payment reference"
        );
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

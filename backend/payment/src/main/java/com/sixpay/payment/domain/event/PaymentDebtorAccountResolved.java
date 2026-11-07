package com.sixpay.payment.domain.event;

import java.time.Instant;
import java.util.Objects;

/**
 * A verified debtor-account reference has been associated with the Payment.
 * Never includes a raw bank account number or an integration access token.
 */
public record PaymentDebtorAccountResolved(
        PaymentEventMetadata metadata,
        String accountBindingFingerprint,
        String maskedDebtorAccountReference,
        Instant resolvedAt
) implements PaymentDomainEvent {
    public PaymentDebtorAccountResolved {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        accountBindingFingerprint = Objects.requireNonNull(
                accountBindingFingerprint, "Account binding fingerprint");
        if (!accountBindingFingerprint.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("Invalid account binding fingerprint");
        }
        maskedDebtorAccountReference = Objects.requireNonNull(
                maskedDebtorAccountReference, "Masked debtor account");
        if (maskedDebtorAccountReference.isBlank()) {
            throw new IllegalArgumentException("Masked debtor account must not be blank");
        }
        resolvedAt = Objects.requireNonNull(resolvedAt, "Resolution instant");
    }
}

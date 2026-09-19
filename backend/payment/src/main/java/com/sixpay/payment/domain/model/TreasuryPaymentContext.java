package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Treasury-payment-specific business context.
 *
 * <p>This context is not a universal Payment invariant. It groups the
 * Treasury claim classification and taxpayer identifier while Treasury
 * beneficiary allocation remains represented by {@link TreasuryAllocationIntent}.</p>
 */
public record TreasuryPaymentContext(
        ClaimType claimType,
        String taxpayerIdentifier
) implements ValueObject {
    public TreasuryPaymentContext {
        claimType = Objects.requireNonNull(claimType, "Treasury claim type");
        taxpayerIdentifier = requireText(taxpayerIdentifier, 64, "Taxpayer identifier");
    }
    private static String requireText(String value, int maximumLength, String label) {
        if (value == null) throw new IllegalArgumentException(label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        if (normalized.length() > maximumLength) throw new IllegalArgumentException(label + " exceeds " + maximumLength + " characters");
        return normalized;
    }
}

package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Safe linked Payment view exposed by the Observed Customer query model.
 */
public record ObservedCustomerPaymentView(
        UUID paymentId,
        String paymentReference,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        ObservedPaymentStatus status,
        String reasonCode,
        Instant createdAt,
        Instant updatedAt
) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerPaymentView {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId is required"
        );

        if (NIL_UUID.equals(paymentId)) {
            throw new IllegalArgumentException(
                    "paymentId must not be nil"
            );
        }

        paymentReference = requiredText(
                paymentReference,
                128,
                "paymentReference"
        );

        financialInstitutionCode = requiredText(
                financialInstitutionCode,
                32,
                "financialInstitutionCode"
        );

        amount = Objects.requireNonNull(
                amount,
                "amount is required"
        );

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "amount must not be negative"
            );
        }

        currency = requiredText(
                currency,
                3,
                "currency"
        ).toUpperCase(java.util.Locale.ROOT);

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "currency must be a valid ISO-4217 code",
                    exception
            );
        }

        status = Objects.requireNonNull(
                status,
                "status is required"
        );

        reasonCode = optionalText(
                reasonCode,
                64,
                "reasonCode"
        );

        createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt is required"
        );

        updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "updatedAt must not precede createdAt"
            );
        }
    }

    private static String requiredText(
            String value,
            int maxLength,
            String field
    ) {
        String normalized = optionalText(
                value,
                maxLength,
                field
        );

        if (normalized == null) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return normalized;
    }

    private static String optionalText(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return normalized;
    }
}

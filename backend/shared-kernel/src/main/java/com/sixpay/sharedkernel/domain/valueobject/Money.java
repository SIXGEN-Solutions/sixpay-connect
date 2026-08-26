package com.sixpay.sharedkernel.domain.valueobject;

import com.sixpay.common.validation.Preconditions;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Represents a monetary amount in a specific currency.
 *
 * @param amount monetary amount
 * @param currency monetary currency
 */
public record Money(
        BigDecimal amount,
        Currency currency
) implements ValueObject {

    public Money {
        amount = normalize(
                Preconditions.requireNonNull(
                        amount,
                        "Money amount must not be null"
                )
        );

        currency = Preconditions.requireNonNull(
                currency,
                "Money currency must not be null"
        );
    }

    /**
     * Creates a monetary value from an amount and currency code.
     *
     * @param amount amount
     * @param currencyCode ISO 4217 currency code
     * @return monetary value
     */
    public static Money of(
            BigDecimal amount,
            String currencyCode
    ) {
        String validatedCurrencyCode =
                Preconditions.requireNonBlank(
                        currencyCode,
                        "Currency code must not be blank"
                );

        return new Money(
                amount,
                Currency.getInstance(validatedCurrencyCode)
        );
    }

    /**
     * Creates a zero monetary value.
     *
     * @param currency currency
     * @return zero monetary value
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Adds another amount using the same currency.
     *
     * @param other amount to add
     * @return addition result
     */
    public Money add(Money other) {
        requireSameCurrency(other);

        return new Money(
                amount.add(other.amount),
                currency
        );
    }

    /**
     * Subtracts another amount using the same currency.
     *
     * @param other amount to subtract
     * @return subtraction result
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);

        return new Money(
                amount.subtract(other.amount),
                currency
        );
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        Preconditions.requireNonNull(
                other,
                "Money value must not be null"
        );

        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on monetary values with different currencies"
            );
        }
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value.signum() == 0) {
            return BigDecimal.ZERO;
        }

        return value.stripTrailingZeros();
    }
}
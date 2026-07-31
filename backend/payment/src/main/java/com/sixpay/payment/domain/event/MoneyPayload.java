package com.sixpay.payment.domain.event;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Explicit serialization-safe monetary payload.
 */
public record MoneyPayload(
        String amount,
        String currency
) implements ValueObject {

    public MoneyPayload {
        Objects.requireNonNull(amount, "Amount");
        Objects.requireNonNull(currency, "Currency");
        if (amount.isBlank() || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Money payload fields must not be blank"
            );
        }
    }

    public static MoneyPayload from(Money money) {
        Objects.requireNonNull(money, "Money");
        return new MoneyPayload(
                money.amount().toPlainString(),
                money.currency().getCurrencyCode()
        );
    }
}

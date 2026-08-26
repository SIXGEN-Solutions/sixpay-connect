package com.sixpay.partner.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public record ValidationThreshold(
        String transactionType,
        Money limit,
        int validationLevels
) {

    public ValidationThreshold(
            String transactionType,
            String currency,
            BigDecimal amount,
            int validationLevels
    ) {
        this(
                transactionType,
                Money.of(amount, normalizeCurrency(currency)),
                validationLevels
        );
    }

    public ValidationThreshold {
        transactionType = AuthorizedPerimeter.normalize(transactionType);
        limit = Objects.requireNonNull(limit, "validation threshold limit is required");
        if (!limit.isPositive()) {
            throw new IllegalArgumentException("validation threshold amount must be positive");
        }
        if (validationLevels < 1) {
            throw new IllegalArgumentException("validation levels must be at least one");
        }
    }

    public String currency() {
        return limit.currency().getCurrencyCode();
    }

    public BigDecimal amount() {
        return limit.amount();
    }

    public boolean requiresMultiLevelValidation(BigDecimal transactionAmount, String transactionCurrency) {
        if (transactionAmount == null || transactionAmount.signum() < 0) {
            throw new IllegalArgumentException("transaction amount must be zero or positive");
        }
        var normalizedCurrency = normalizeCurrency(transactionCurrency);
        if (!currency().equals(normalizedCurrency)) {
            throw new IllegalArgumentException("transaction currency does not match validation threshold currency");
        }
        return validationLevels > 1 && transactionAmount.compareTo(amount()) > 0;
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        return Currency.getInstance(
                currency.strip().toUpperCase(Locale.ROOT)
        ).getCurrencyCode();
    }
}

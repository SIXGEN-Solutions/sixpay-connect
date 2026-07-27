package com.sixpay.partner.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class PartnerThresholdEmbeddable {

    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "threshold_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "validation_levels", nullable = false)
    private int validationLevels;

    protected PartnerThresholdEmbeddable() {
    }

    public PartnerThresholdEmbeddable(
            String transactionType,
            String currency,
            BigDecimal amount,
            int validationLevels
    ) {
        this.transactionType = transactionType;
        this.currency = currency;
        this.amount = amount;
        this.validationLevels = validationLevels;
    }

    public String transactionType() {
        return transactionType;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public int validationLevels() {
        return validationLevels;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof PartnerThresholdEmbeddable that
                && Objects.equals(transactionType, that.transactionType)
                && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionType, currency);
    }
}

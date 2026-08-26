package com.sixpay.sharedkernel.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void shouldCreateMoney() {
        Money money = Money.of(
                new BigDecimal("1000.00"),
                "XAF"
        );

        assertEquals(new BigDecimal("1E+3"), money.amount());
        assertEquals(
                Currency.getInstance("XAF"),
                money.currency()
        );
    }

    @Test
    void shouldNormalizeEquivalentAmounts() {
        Money first = Money.of(
                new BigDecimal("1000.00"),
                "XAF"
        );

        Money second = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        assertEquals(first, second);
    }

    @Test
    void shouldAddAmountsUsingSameCurrency() {
        Money first = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        Money second = Money.of(
                new BigDecimal("500"),
                "XAF"
        );

        Money result = first.add(second);

        assertEquals(
                Money.of(new BigDecimal("1500"), "XAF"),
                result
        );
    }

    @Test
    void shouldSubtractAmountsUsingSameCurrency() {
        Money first = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        Money second = Money.of(
                new BigDecimal("500"),
                "XAF"
        );

        Money result = first.subtract(second);

        assertEquals(
                Money.of(new BigDecimal("500"), "XAF"),
                result
        );
    }

    @Test
    void shouldRejectOperationWithDifferentCurrencies() {
        Money xaf = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        Money cad = Money.of(
                new BigDecimal("1000"),
                "CAD"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> xaf.add(cad)
        );
    }

    @Test
    void shouldIdentifyPositiveAmount() {
        Money money = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        assertTrue(money.isPositive());
    }

    @Test
    void shouldIdentifyZeroAmount() {
        Money money = Money.zero(
                Currency.getInstance("XAF")
        );

        assertTrue(money.isZero());
    }
}
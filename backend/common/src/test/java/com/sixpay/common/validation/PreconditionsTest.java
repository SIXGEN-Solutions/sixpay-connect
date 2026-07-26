package com.sixpay.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreconditionsTest {

    @Test
    void shouldReturnNonNullValue() {
        String value = Preconditions.requireNonNull(
                "sixpay",
                "Value is required"
        );

        assertEquals("sixpay", value);
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(
                NullPointerException.class,
                () -> Preconditions.requireNonNull(
                        null,
                        "Value is required"
                )
        );
    }

    @Test
    void shouldReturnNonBlankValue() {
        String value = Preconditions.requireNonBlank(
                "sixpay",
                "Value is required"
        );

        assertEquals("sixpay", value);
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.requireNonBlank(
                        " ",
                        "Value is required"
                )
        );
    }

    @Test
    void shouldReturnPositiveValue() {
        long value = Preconditions.requirePositive(
                10,
                "Value must be positive"
        );

        assertEquals(10, value);
    }

    @Test
    void shouldRejectZeroValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.requirePositive(
                        0,
                        "Value must be positive"
                )
        );
    }

    @Test
    void shouldRejectFalseCondition() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.requireTrue(
                        false,
                        "Condition must be true"
                )
        );
    }
}
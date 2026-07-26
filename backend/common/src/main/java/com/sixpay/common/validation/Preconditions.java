package com.sixpay.common.validation;

import java.util.Objects;

/**
 * Provides basic validation methods for public contracts and
 * technical components.
 */
public final class Preconditions {

    private Preconditions() {
        throw new IllegalStateException(
                "Preconditions is a utility class and cannot be instantiated"
        );
    }

    /**
     * Verifies that a value is not null.
     *
     * @param value value to validate
     * @param message exception message
     * @param <T> value type
     * @return validated value
     */
    public static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    /**
     * Verifies that a string is not null or blank.
     *
     * @param value value to validate
     * @param message exception message
     * @return validated value
     */
    public static String requireNonBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    /**
     * Verifies that a number is strictly positive.
     *
     * @param value value to validate
     * @param message exception message
     * @return validated value
     */
    public static long requirePositive(
            long value,
            String message
    ) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    /**
     * Verifies that a condition is true.
     *
     * @param condition condition to validate
     * @param message exception message
     */
    public static void requireTrue(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
package com.sixpay.customer.observation.application.query;

/**
 * Opaque, versioned cursor value owned by Customer Observation.
 *
 * <p>The application model never interprets or logs the encoded cursor.</p>
 */
public record ObservedCustomerCursor(String value) {

    public static final int MAX_LENGTH = 2048;

    public ObservedCustomerCursor {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "cursor value must not be blank"
            );
        }

        value = value.strip();

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "cursor value must not exceed "
                            + MAX_LENGTH
                            + " characters"
            );
        }
    }

    @Override
    public String toString() {
        return "ObservedCustomerCursor[value=[PROTECTED]]";
    }
}

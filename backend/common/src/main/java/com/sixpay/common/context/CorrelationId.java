package com.sixpay.common.context;

import com.sixpay.common.validation.Preconditions;

import java.util.UUID;

/**
 * Identifies and correlates operations across the different components
 * participating input the processing of a request.
 *
 * @param value correlation identifier value
 */
public record CorrelationId(String value) {

    public CorrelationId {
        value = Preconditions.requireNonBlank(
                value,
                "Correlation ID must not be blank"
        );
    }

    /**
     * Creates a correlation identifier from an existing value.
     *
     * @param value existing identifier
     * @return correlation identifier
     */
    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }

    /**
     * Generates a new correlation identifier.
     *
     * @return generated correlation identifier
     */
    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
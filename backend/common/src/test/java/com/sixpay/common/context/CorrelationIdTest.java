package com.sixpay.common.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdTest {

    @Test
    void shouldCreateCorrelationIdFromExistingValue() {
        CorrelationId correlationId =
                CorrelationId.of("correlation-123");

        assertEquals("correlation-123", correlationId.value());
        assertEquals("correlation-123", correlationId.toString());
    }

    @Test
    void shouldGenerateCorrelationId() {
        CorrelationId correlationId = CorrelationId.generate();

        assertFalse(correlationId.value().isBlank());
    }

    @Test
    void shouldRejectBlankCorrelationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CorrelationId.of(" ")
        );
    }

    @Test
    void shouldRejectNullCorrelationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CorrelationId.of(null)
        );
    }
}
package com.sixpay.integration.exception;

import com.sixpay.integration.system.ExternalSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSystemExceptionTest {

    @Test
    void shouldCreateRetryableIntegrationException() {
        ExternalSystemException exception =
                new ExternalSystemException(
                        ExternalSystem.AMPLITUDE,
                        IntegrationErrorType.TIMEOUT,
                        true,
                        "Amplitude request timed output"
                );

        assertEquals(
                ExternalSystem.AMPLITUDE,
                exception.externalSystem()
        );

        assertEquals(
                IntegrationErrorType.TIMEOUT,
                exception.errorType()
        );

        assertTrue(exception.retryable());

        assertEquals(
                "Amplitude request timed output",
                exception.getMessage()
        );
    }
}
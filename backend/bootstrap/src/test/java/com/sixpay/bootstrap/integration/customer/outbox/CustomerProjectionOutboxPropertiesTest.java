package com.sixpay.bootstrap.integration.customer.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerProjectionOutboxPropertiesTest {

    @Test
    void acceptsBoundedProductionConfiguration() {
        assertDoesNotThrow(() ->
                properties(
                        true,
                        50,
                        Duration.ofSeconds(1),
                        10,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );
    }

    @Test
    void rejectsInvalidBatchAndDurations() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        true,
                        0,
                        Duration.ofSeconds(1),
                        10,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        true,
                        501,
                        Duration.ofSeconds(1),
                        10,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        true,
                        50,
                        Duration.ZERO,
                        10,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        true,
                        50,
                        Duration.ofSeconds(1),
                        0,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        true,
                        50,
                        Duration.ofMinutes(3),
                        10,
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(2)
                )
        );
    }

    private static CustomerProjectionOutboxProperties properties(
            boolean enabled,
            int batchSize,
            Duration pollingInterval,
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff,
            Duration processingTimeout
    ) {
        return new CustomerProjectionOutboxProperties(
                enabled,
                batchSize,
                pollingInterval,
                maxAttempts,
                initialBackoff,
                maxBackoff,
                processingTimeout
        );
    }
}

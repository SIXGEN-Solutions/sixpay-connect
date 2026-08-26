package com.sixpay.customer.observation.infrastructure.observability;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservedCustomerProjectionMetricsTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T17:00:00Z");

    @Test
    void recordsBoundedSuccessReplayStaleAndLagMetrics() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        ObservedCustomerProjectionMetrics metrics =
                new ObservedCustomerProjectionMetrics(
                        registry
                );
        ObserveCustomerCommand command =
                command(ObservedPaymentStatus.RECEIVED);

        metrics.begin(command);
        metrics.success(
                command,
                result(
                        ObserveCustomerResult.Disposition.REPLAYED
                ),
                Duration.ofMillis(12),
                Duration.ofMillis(40)
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .REQUESTS
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_RECEIVED"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .RESULTS
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_RECEIVED"
                        )
                        .tag("result", "REPLAYED")
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .REPLAYS
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_RECEIVED"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1L,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .LAG
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_RECEIVED"
                        )
                        .tag("result", "REPLAYED")
                        .timer()
                        .count()
        );
    }

    @Test
    void retryUsesBoundedErrorAndAttemptTags() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        ObservedCustomerProjectionMetrics metrics =
                new ObservedCustomerProjectionMetrics(
                        registry
                );
        ObserveCustomerCommand command =
                command(ObservedPaymentStatus.DEBITED);

        metrics.retry(
                command,
                3,
                ObservedCustomerProjectionErrorType
                        .OPTIMISTIC_LOCK
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .RETRIES
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_DEBITED"
                        )
                        .tag(
                                "error_type",
                                "OPTIMISTIC_LOCK"
                        )
                        .tag(
                                "attempt_bucket",
                                "3_PLUS"
                        )
                        .counter()
                        .count()
        );
    }

    private static ObserveCustomerResult result(
            ObserveCustomerResult.Disposition disposition
    ) {
        return new ObserveCustomerResult(
                ObservedCustomerId.of(
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                ),
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                disposition,
                1,
                NOW
        );
    }

    private static ObserveCustomerCommand command(
            ObservedPaymentStatus status
    ) {
        return new ObserveCustomerCommand(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                "PAY-001",
                "M0123456",
                "Société ABC",
                "***-***-1234",
                "a***@example.com",
                "BANK",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("100.00"),
                "XAF",
                status,
                null,
                NOW.minusSeconds(10),
                NOW,
                NOW,
                "55555555-5555-4555-8555-555555555555"
        );
    }
}

package com.sixpay.customer.observation.infrastructure.observability;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerProjectionObservationTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T17:00:00Z");

    @Test
    void delegatesReturnsResultAndRecordsSuccess() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        ObservedCustomerProjectionMetrics metrics =
                new ObservedCustomerProjectionMetrics(
                        registry
                );
        ObserveCustomerCommand command = command();
        ObserveCustomerResult expected =
                result();

        ObserveCustomerUseCase delegate =
                ignored -> expected;

        ObservedCustomerProjectionObservation observation =
                new ObservedCustomerProjectionObservation(
                        delegate,
                        metrics,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertSame(
                expected,
                observation.observe(command)
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
                        .tag("result", "APPLIED")
                        .counter()
                        .count()
        );
    }

    @Test
    void recordsFailureAndPreservesOriginalException() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        ObservedCustomerProjectionMetrics metrics =
                new ObservedCustomerProjectionMetrics(
                        registry
                );
        IllegalStateException failure =
                new IllegalStateException("temporary");

        ObservedCustomerProjectionObservation observation =
                new ObservedCustomerProjectionObservation(
                        ignored -> {
                            throw failure;
                        },
                        metrics,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertSame(
                failure,
                assertThrows(
                        IllegalStateException.class,
                        () -> observation.observe(command())
                )
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerProjectionMetrics
                                        .FAILURES
                        )
                        .tag(
                                "event_type",
                                "PAYMENT_RECEIVED"
                        )
                        .tag("result", "FAILED")
                        .tag("error_type", "UNEXPECTED")
                        .counter()
                        .count()
        );
    }

    private static ObserveCustomerResult result() {
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
                ObserveCustomerResult.Disposition.APPLIED,
                1,
                NOW
        );
    }

    private static ObserveCustomerCommand command() {
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
                ObservedPaymentStatus.RECEIVED,
                null,
                NOW.minusSeconds(10),
                NOW,
                NOW,
                "55555555-5555-4555-8555-555555555555"
        );
    }
}

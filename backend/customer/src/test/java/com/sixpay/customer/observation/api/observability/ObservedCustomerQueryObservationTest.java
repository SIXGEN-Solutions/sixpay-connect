package com.sixpay.customer.observation.api.observability;

import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerQueryObservationTest {

    private static final Clock CLOCK =
            Clock.fixed(
                    Instant.parse("2026-08-05T03:00:00Z"),
                    ZoneOffset.UTC
            );

    @Test
    void successRecordsRequestResultAndDuration() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        ObservedCustomerQueryObservation observation =
                new ObservedCustomerQueryObservation(
                        registry,
                        CLOCK
                );

        String result = observation.observe(
                ObservedCustomerQueryOperation.SEARCH,
                "corr-001",
                null,
                50,
                () -> "ok",
                value ->
                        ObservedCustomerQueryObservation
                                .ResultMetadata
                                .page(true)
        );

        assertEquals("ok", result);
        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerQueryObservation
                                        .REQUESTS
                        )
                        .tag("operation", "SEARCH")
                        .counter()
                        .count()
        );
        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerQueryObservation
                                        .RESULTS
                        )
                        .tag("operation", "SEARCH")
                        .tag("result", "SUCCESS")
                        .counter()
                        .count()
        );
    }

    @Test
    void temporaryFailureUsesBoundedFailureTags() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        ObservedCustomerQueryObservation observation =
                new ObservedCustomerQueryObservation(
                        registry,
                        CLOCK
                );

        assertThrows(
                ObservedCustomerQueryUnavailableException.class,
                () -> observation.observe(
                        ObservedCustomerQueryOperation.GET,
                        "corr-002",
                        null,
                        null,
                        () -> {
                            throw new
                                    ObservedCustomerQueryUnavailableException(
                                            "temporary"
                                    );
                        },
                        value ->
                                ObservedCustomerQueryObservation
                                        .ResultMetadata
                                        .none()
                )
        );

        assertEquals(
                1.0,
                registry.get(
                                ObservedCustomerQueryObservation
                                        .FAILURES
                        )
                        .tag("operation", "GET")
                        .tag("result", "UNAVAILABLE")
                        .tag(
                                "error_type",
                                "TEMPORARY_UNAVAILABLE"
                        )
                        .counter()
                        .count()
        );
    }
}

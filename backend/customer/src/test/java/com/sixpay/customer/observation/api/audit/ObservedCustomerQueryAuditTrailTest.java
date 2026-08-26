package com.sixpay.customer.observation.api.audit;

import com.sixpay.customer.observation.api.observability.*;
import com.sixpay.customer.observation.application.audit.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerQueryAuditTrailTest {

    @Test
    void auditFailureDoesNotFailTheQueryPath() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        ObservedCustomerQueryAuditTrail trail =
                new ObservedCustomerQueryAuditTrail(
                        record -> {
                            throw new IllegalStateException(
                                    "audit unavailable"
                            );
                        },
                        java.util.UUID::randomUUID,
                        registry,
                        Clock.fixed(
                                Instant.parse(
                                        "2026-08-05T16:00:00Z"
                                ),
                                ZoneOffset.UTC
                        )
                );

        assertDoesNotThrow(
                () -> trail.success(
                        ObservedCustomerQueryOperation.SEARCH,
                        null,
                        "55555555-5555-4555-8555-555555555555"
                )
        );

        assertEquals(
                1.0,
                registry.get(
                        ObservedCustomerQueryAuditTrail
                                .AUDIT_FAILURES
                )
                        .counter()
                        .count()
        );
    }

    @Test
    void unavailableQueryUsesTechnicalFailureReason() {
        AtomicReference<ObservedCustomerAuditRecord> captured =
                new AtomicReference<>();

        ObservedCustomerQueryAuditTrail trail =
                new ObservedCustomerQueryAuditTrail(
                        captured::set,
                        java.util.UUID::randomUUID,
                        new SimpleMeterRegistry(),
                        Clock.systemUTC()
                );

        trail.failure(
                ObservedCustomerQueryOperation.GET,
                ObservedCustomerQueryResult.UNAVAILABLE,
                java.util.UUID.fromString(
                        "44444444-4444-4444-8444-444444444444"
                ),
                "55555555-5555-4555-8555-555555555555"
        );

        assertEquals(
                ObservedCustomerAuditAction.QUERY_FAILED,
                captured.get().action()
        );
        assertEquals(
                "GET_UNAVAILABLE",
                captured.get().reasonCode()
        );
    }
}

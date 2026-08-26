package com.sixpay.customer.verification.infrastructure.banking.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BankingVerificationObservationTest {

    @Test
    void recordsLowCardinalitySuccessRetryAndDurationMetrics() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        BankingVerificationObservation observation =
                new BankingVerificationObservation(registry);

        long startedAt = observation.start();
        observation.success(
                "AMPLITUDE",
                3,
                startedAt
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.requests"
                        )
                        .tag("institution", "AMPLITUDE")
                        .tag("outcome", "success")
                        .counter()
                        .count()
        );
        assertEquals(
                2.0,
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.retries"
                        )
                        .tag("institution", "AMPLITUDE")
                        .counter()
                        .count()
        );
        assertNotNull(
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.duration"
                        )
                        .tag("institution", "AMPLITUDE")
                        .tag("outcome", "success")
                        .timer()
        );
    }

    @Test
    void recordsBoundedErrorTypeWithoutIdentifiersAsTags() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();
        BankingVerificationObservation observation =
                new BankingVerificationObservation(registry);

        long startedAt = observation.start();
        observation.failure(
                "AMPLITUDE",
                "timeout",
                1,
                startedAt
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.errors"
                        )
                        .tag("institution", "AMPLITUDE")
                        .tag("error_type", "timeout")
                        .counter()
                        .count()
        );
    }
}

package com.sixpay.customer.observation.infrastructure.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerProjectionRetryPolicyTest {

    @Test
    void computesBoundedExponentialBackoffWithoutJitter() {
        ObservedCustomerProjectionRetryPolicy policy =
                new ObservedCustomerProjectionRetryPolicy(
                        4,
                        Duration.ofMillis(10),
                        Duration.ofMillis(25),
                        2.0,
                        0.0,
                        ignored -> {
                        },
                        () -> 0.5
                );

        assertEquals(
                Duration.ofMillis(10),
                policy.delayAfter(1)
        );
        assertEquals(
                Duration.ofMillis(20),
                policy.delayAfter(2)
        );
        assertEquals(
                Duration.ofMillis(25),
                policy.delayAfter(3)
        );
    }

    @Test
    void jitterRemainsInsideConfiguredBounds() {
        ObservedCustomerProjectionRetryPolicy low =
                policyWithRandom(0.0);
        ObservedCustomerProjectionRetryPolicy high =
                policyWithRandom(
                        Math.nextDown(1.0)
                );

        assertEquals(
                Duration.ofMillis(8),
                low.delayAfter(1)
        );
        assertTrue(
                high.delayAfter(1)
                        .compareTo(Duration.ofMillis(12)) <= 0
        );
    }

    @Test
    void retriesOnlyRetryableFailuresBeforeLastAttempt() {
        ObservedCustomerProjectionRetryPolicy policy =
                policyWithRandom(0.5);

        assertTrue(
                policy.shouldRetry(
                        1,
                        ObservedCustomerProjectionFailureType
                                .DEADLOCK
                )
        );
        assertFalse(
                policy.shouldRetry(
                        3,
                        ObservedCustomerProjectionFailureType
                                .DEADLOCK
                )
        );
        assertFalse(
                policy.shouldRetry(
                        1,
                        ObservedCustomerProjectionFailureType
                                .INVALID_PAYLOAD
                )
        );
    }

    @Test
    void waitingIsDelegatedToInjectedBackoff() {
        List<Duration> delays = new ArrayList<>();

        ObservedCustomerProjectionRetryPolicy policy =
                new ObservedCustomerProjectionRetryPolicy(
                        3,
                        Duration.ofMillis(10),
                        Duration.ofMillis(250),
                        2.0,
                        0.0,
                        delays::add,
                        () -> 0.5
                );

        policy.beforeRetry(2);

        assertEquals(
                List.of(Duration.ofMillis(20)),
                delays
        );
    }

    private static ObservedCustomerProjectionRetryPolicy
    policyWithRandom(double random) {
        return new ObservedCustomerProjectionRetryPolicy(
                3,
                Duration.ofMillis(10),
                Duration.ofMillis(250),
                2.0,
                0.20,
                ignored -> {
                },
                () -> random
        );
    }
}

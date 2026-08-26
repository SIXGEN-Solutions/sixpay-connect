package com.sixpay.notification.domain.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationRetryPolicyTest {

    @Test
    void appliesBoundedExponentialBackoff() {
        var policy =
                new OperationalNotificationRetryPolicy(
                        5,
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(2)
                );

        Instant failedAt =
                Instant.parse(
                        "2026-08-07T16:00:00Z"
                );

        assertEquals(
                failedAt.plusSeconds(30),
                policy.nextAttemptAt(
                        failedAt,
                        1
                )
        );

        assertEquals(
                failedAt.plusSeconds(60),
                policy.nextAttemptAt(
                        failedAt,
                        2
                )
        );

        assertEquals(
                failedAt.plusSeconds(120),
                policy.nextAttemptAt(
                        failedAt,
                        3
                )
        );

        assertEquals(
                failedAt.plusSeconds(120),
                policy.nextAttemptAt(
                        failedAt,
                        4
                )
        );

        assertFalse(policy.exhausted(4));
        assertTrue(policy.exhausted(5));
    }
}

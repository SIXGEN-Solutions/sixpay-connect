package com.sixpay.notification.application.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRetryPolicyTest {

    private static final Instant NOW =
            Instant.parse("2026-07-28T12:00:00Z");

    private final NotificationRetryPolicy policy =
            new NotificationRetryPolicy(
                    5,
                    Duration.ofMinutes(1),
                    2.0,
                    Duration.ofMinutes(5)
            );

    @Test
    void appliesExponentialBackoffCappedAtMaximumDelay() {
        assertThat(policy.nextAttemptAt(NOW, 1))
                .isEqualTo(NOW.plusSeconds(60));
        assertThat(policy.nextAttemptAt(NOW, 2))
                .isEqualTo(NOW.plusSeconds(120));
        assertThat(policy.nextAttemptAt(NOW, 3))
                .isEqualTo(NOW.plusSeconds(240));
        assertThat(policy.nextAttemptAt(NOW, 4))
                .isEqualTo(NOW.plusSeconds(300));
    }

    @Test
    void becomesExhaustedAtConfiguredMaximum() {
        assertThat(policy.exhausted(4)).isFalse();
        assertThat(policy.exhausted(5)).isTrue();
    }
}

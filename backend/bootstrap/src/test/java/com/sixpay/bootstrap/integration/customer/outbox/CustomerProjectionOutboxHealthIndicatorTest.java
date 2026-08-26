package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerProjectionOutboxHealthIndicatorTest {

    private static final Instant NOW =
            Instant.parse("2026-08-04T21:00:00Z");

    @Test
    void reportsBacklogCountAndOldestLag() {
        PaymentOutboxRepository repository =
                mock(PaymentOutboxRepository.class);

        when(repository.countOutstandingByEventType(
                "payment.observation-projection"
        )).thenReturn(12L);

        when(repository.findOldestOutstandingOccurredAt(
                "payment.observation-projection"
        )).thenReturn(
                Optional.of(
                        NOW.minusSeconds(90)
                )
        );

        var indicator =
                new CustomerProjectionOutboxHealthIndicator(
                        repository,
                        properties(true),
                        Clock.fixed(
                                NOW,
                                ZoneOffset.UTC
                        )
                );

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(
                12L,
                health.getDetails().get(
                        "outstandingEvents"
                )
        );
        assertEquals(
                90L,
                health.getDetails().get(
                        "oldestEventLagSeconds"
                )
        );
    }

    @Test
    void disabledDispatcherIsReportedWithoutDatabaseAccess() {
        PaymentOutboxRepository repository =
                mock(PaymentOutboxRepository.class);

        var indicator =
                new CustomerProjectionOutboxHealthIndicator(
                        repository,
                        properties(false),
                        Clock.fixed(
                                NOW,
                                ZoneOffset.UTC
                        )
                );

        var health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(
                false,
                health.getDetails().get("enabled")
        );
    }

    private static CustomerProjectionOutboxProperties properties(
            boolean enabled
    ) {
        return new CustomerProjectionOutboxProperties(
                enabled,
                50,
                Duration.ofSeconds(1),
                10,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(2)
        );
    }
}

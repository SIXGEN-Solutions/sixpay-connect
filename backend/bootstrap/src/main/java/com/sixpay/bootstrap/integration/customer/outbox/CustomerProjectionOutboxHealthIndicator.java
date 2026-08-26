package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import com.sixpay.payment.infrastructure.outbox.serialization
        .PaymentOutboxEventTypeRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class CustomerProjectionOutboxHealthIndicator
        implements HealthIndicator {

    private static final String EVENT_TYPE =
            PaymentOutboxEventTypeRegistry
                    .OBSERVED_CUSTOMER_PROJECTION_TYPE;

    private final PaymentOutboxRepository repository;
    private final CustomerProjectionOutboxProperties properties;
    private final Clock clock;

    public CustomerProjectionOutboxHealthIndicator(
            PaymentOutboxRepository repository,
            CustomerProjectionOutboxProperties properties,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("eventType", EVENT_TYPE)
                    .build();
        }

        try {
            long outstanding =
                    repository.countOutstandingByEventType(
                            EVENT_TYPE
                    );

            Instant now = clock.instant();

            long lagSeconds = repository
                    .findOldestOutstandingOccurredAt(EVENT_TYPE)
                    .map(oldest ->
                            oldest.isAfter(now)
                                    ? 0L
                                    : Duration.between(
                                            oldest,
                                            now
                                    ).toSeconds()
                    )
                    .orElse(0L);

            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("eventType", EVENT_TYPE)
                    .withDetail(
                            "outstandingEvents",
                            outstanding
                    )
                    .withDetail(
                            "oldestEventLagSeconds",
                            lagSeconds
                    )
                    .withDetail(
                            "batchSize",
                            properties.batchSize()
                    )
                    .withDetail(
                            "maxAttempts",
                            properties.maxAttempts()
                    )
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception)
                    .withDetail("enabled", true)
                    .withDetail("eventType", EVENT_TYPE)
                    .build();
        }
    }
}

package com.sixpay.customer.observation.infrastructure.health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public final class ObservedCustomerProjectionHealthIndicator
        implements HealthIndicator {

    static final String PROJECTION_FAILURES =
            "sixpay.customer.observation.projection.failures";

    private static final Duration DEGRADED_LAG = Duration.ofMinutes(5);
    private static final Duration DOWN_LAG = Duration.ofMinutes(15);

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public ObservedCustomerProjectionHealthIndicator(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry
    ) {
        this(jdbcTemplate, meterRegistry, Clock.systemUTC());
    }

    ObservedCustomerProjectionHealthIndicator(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Health health() {
        try {
            Instant lastProcessedEventAt = lastProcessedEventAt();
            Duration projectionLag = nonNegativeLag(lastProcessedEventAt);
            long retryExhaustedCount = failureCount(
                    "OPTIMISTIC_LOCK",
                    "TRANSACTION"
            );
            long deadLetterCount = failureCount(
                    "DOMAIN",
                    "DATA_INTEGRITY",
                    "UNEXPECTED"
            );

            return status(
                    projectionLag,
                    retryExhaustedCount,
                    deadLetterCount
            )
                    .withDetail("databaseReachable", true)
                    .withDetail(
                            "lastProcessedEventAt",
                            lastProcessedEventAt == null
                                    ? "NONE"
                                    : lastProcessedEventAt
                    )
                    .withDetail("projectionLagMs", projectionLag.toMillis())
                    .withDetail("retryExhaustedCount", retryExhaustedCount)
                    .withDetail("deadLetterCount", deadLetterCount)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down()
                    .withDetail("databaseReachable", false)
                    .withDetail(
                            "reason",
                            "PROJECTION_HEALTH_CHECK_FAILED"
                    )
                    .build();
        }
    }

    private Instant lastProcessedEventAt() {
        Timestamp value = jdbcTemplate.queryForObject(
                """
                SELECT MAX(processed_at)
                FROM customer_observation_processed_event
                """,
                Timestamp.class
        );
        return value == null ? null : value.toInstant();
    }

    private Duration nonNegativeLag(Instant value) {
        if (value == null) {
            return Duration.ZERO;
        }
        Duration lag = Duration.between(value, clock.instant());
        return lag.isNegative() ? Duration.ZERO : lag;
    }

    private Health.Builder status(
            Duration projectionLag,
            long retryExhaustedCount,
            long deadLetterCount
    ) {
        if (projectionLag.compareTo(DOWN_LAG) > 0
                || deadLetterCount > 0) {
            return Health.down();
        }
        if (projectionLag.compareTo(DEGRADED_LAG) > 0
                || retryExhaustedCount > 0) {
            return Health.status("DEGRADED");
        }
        return Health.up();
    }

    private long failureCount(String... errorTypes) {
        double total = 0;
        for (Meter meter : meterRegistry.find(PROJECTION_FAILURES).meters()) {
            if (!(meter instanceof Counter counter)) {
                continue;
            }
            String errorType = meter.getId().getTag("error_type");
            if (contains(errorTypes, errorType)) {
                total += counter.count();
            }
        }
        return Math.round(total);
    }

    private static boolean contains(String[] values, String candidate) {
        if (candidate == null) {
            return false;
        }
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}

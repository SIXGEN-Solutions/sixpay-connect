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
public final class ObservedCustomerQueryHealthIndicator
        implements HealthIndicator {

    static final String QUERY_REQUESTS =
            "sixpay.customer.observation.query.requests";
    static final String QUERY_FAILURES =
            "sixpay.customer.observation.query.failures";

    private static final double DEGRADED_FAILURE_RATE = 0.05D;
    private static final double DOWN_FAILURE_RATE = 0.20D;

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public ObservedCustomerQueryHealthIndicator(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry
    ) {
        this(jdbcTemplate, meterRegistry, Clock.systemUTC());
    }

    ObservedCustomerQueryHealthIndicator(
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
            boolean databaseReachable = databaseReachable();
            Instant oldestProjectionAt = oldestProjectionAt();
            Duration oldestProjectionAge = nonNegativeAge(oldestProjectionAt);
            double requests = counterTotal(QUERY_REQUESTS);
            double failures = counterTotal(QUERY_FAILURES);
            double failureRate = requests <= 0
                    ? 0.0D
                    : Math.min(1.0D, failures / requests);

            return status(databaseReachable, failureRate)
                    .withDetail("databaseReachable", databaseReachable)
                    .withDetail(
                            "oldestProjectionAgeMs",
                            oldestProjectionAge.toMillis()
                    )
                    .withDetail(
                            "queryFailureRate",
                            Math.round(failureRate * 10_000D) / 10_000D
                    )
                    .build();
        } catch (RuntimeException exception) {
            return Health.down()
                    .withDetail("databaseReachable", false)
                    .withDetail("reason", "QUERY_HEALTH_CHECK_FAILED")
                    .build();
        }
    }

    private boolean databaseReachable() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );
        return Integer.valueOf(1).equals(value);
    }

    private Instant oldestProjectionAt() {
        Timestamp value = jdbcTemplate.queryForObject(
                """
                SELECT MIN(last_observed_at)
                FROM customer_observed_customer
                """,
                Timestamp.class
        );
        return value == null ? null : value.toInstant();
    }

    private Duration nonNegativeAge(Instant value) {
        if (value == null) {
            return Duration.ZERO;
        }
        Duration age = Duration.between(value, clock.instant());
        return age.isNegative() ? Duration.ZERO : age;
    }

    private Health.Builder status(
            boolean databaseReachable,
            double failureRate
    ) {
        if (!databaseReachable || failureRate >= DOWN_FAILURE_RATE) {
            return Health.down();
        }
        if (failureRate >= DEGRADED_FAILURE_RATE) {
            return Health.status("DEGRADED");
        }
        return Health.up();
    }

    private double counterTotal(String metricName) {
        double total = 0;
        for (Meter meter : meterRegistry.find(metricName).meters()) {
            if (meter instanceof Counter counter) {
                total += counter.count();
            }
        }
        return total;
    }
}

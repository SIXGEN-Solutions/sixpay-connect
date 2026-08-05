package com.sixpay.customer.observation.infrastructure.health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Component
public final class ObservedCustomerAuditHealthIndicator
        implements HealthIndicator {

    static final String AUDIT_FAILURES =
            "sixpay.customer.observation.audit.failures";

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public ObservedCustomerAuditHealthIndicator(
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    @Override
    public Health health() {
        try {
            boolean repositoryReachable = repositoryReachable();
            Instant lastAuditPersistedAt = lastAuditPersistedAt();
            long auditFailureCount = auditFailureCount();

            return status(repositoryReachable, auditFailureCount)
                    .withDetail("repositoryReachable", repositoryReachable)
                    .withDetail(
                            "lastAuditPersistedAt",
                            lastAuditPersistedAt == null
                                    ? "NONE"
                                    : lastAuditPersistedAt
                    )
                    .withDetail("auditFailureCount", auditFailureCount)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down()
                    .withDetail("repositoryReachable", false)
                    .withDetail("reason", "AUDIT_HEALTH_CHECK_FAILED")
                    .build();
        }
    }

    private boolean repositoryReachable() {
        Integer value = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM customer_observation_audit
                WHERE 1 = 0
                """,
                Integer.class
        );
        return value != null;
    }

    private Instant lastAuditPersistedAt() {
        Timestamp value = jdbcTemplate.queryForObject(
                """
                SELECT MAX(occurred_at)
                FROM customer_observation_audit
                """,
                Timestamp.class
        );
        return value == null ? null : value.toInstant();
    }

    private long auditFailureCount() {
        double total = 0;
        for (Meter meter : meterRegistry.find(AUDIT_FAILURES).meters()) {
            if (meter instanceof Counter counter) {
                total += counter.count();
            }
        }
        return Math.round(total);
    }

    private Health.Builder status(
            boolean repositoryReachable,
            long auditFailureCount
    ) {
        if (!repositoryReachable) {
            return Health.down();
        }
        if (auditFailureCount > 0) {
            return Health.status("DEGRADED");
        }
        return Health.up();
    }
}

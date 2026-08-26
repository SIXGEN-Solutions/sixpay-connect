package com.sixpay.payment.infrastructure.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Payment database and durable-work health.
 *
 * <p>Backlog counts are exposed as details but do not independently mark the
 * module DOWN. Database unavailability marks the component DOWN.</p>
 */
@Component("payment")
@ConditionalOnBean(JdbcTemplate.class)
public final class PaymentHealthIndicator
        implements HealthIndicator {

    private final JdbcTemplate jdbc;

    public PaymentHealthIndicator(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(
                jdbc,
                "JDBC template"
        );
    }

    @Override
    public Health health() {
        try {
            Integer databaseCheck =
                    jdbc.queryForObject(
                            "SELECT 1",
                            Integer.class
                    );

            long pendingOutbox = count(
                    """
                    SELECT COUNT(*)
                      FROM payment_outbox_events
                     WHERE status IN ('PENDING', 'FAILED')
                    """
            );

            long deadOutbox = count(
                    """
                    SELECT COUNT(*)
                      FROM payment_outbox_events
                     WHERE status = 'DEAD'
                    """
            );

            long staleProcessing = count(
                    """
                    SELECT COUNT(*)
                      FROM payment_outbox_events
                     WHERE status = 'PROCESSING'
                       AND claimed_at < ?
                    """,
                    Instant.now().minus(
                            15,
                            ChronoUnit.MINUTES
                    )
            );

            long staleIdempotency = count(
                    """
                    SELECT COUNT(*)
                      FROM payment_idempotency
                     WHERE status = 'IN_PROGRESS'
                       AND updated_at < ?
                    """,
                    Instant.now().minus(
                            15,
                            ChronoUnit.MINUTES
                    )
            );

            return Health.up()
                    .withDetail(
                            "database",
                            databaseCheck != null
                                    && databaseCheck == 1
                                    ? "reachable"
                                    : "unexpected-response"
                    )
                    .withDetail(
                            "pendingOutbox",
                            pendingOutbox
                    )
                    .withDetail(
                            "deadOutbox",
                            deadOutbox
                    )
                    .withDetail(
                            "staleProcessingOutbox",
                            staleProcessing
                    )
                    .withDetail(
                            "staleIdempotency",
                            staleIdempotency
                    )
                    .build();
        } catch (Exception failure) {
            return Health.down(failure)
                    .withDetail(
                            "component",
                            "payment"
                    )
                    .build();
        }
    }

    private long count(
            String sql,
            Object... arguments
    ) {
        Long value = jdbc.queryForObject(
                sql,
                Long.class,
                arguments
        );
        return value == null ? 0L : value;
    }
}

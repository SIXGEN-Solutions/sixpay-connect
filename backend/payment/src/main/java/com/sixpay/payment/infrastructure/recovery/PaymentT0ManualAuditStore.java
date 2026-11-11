package com.sixpay.payment.infrastructure.recovery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Operational audit, separate from immutable financial domain-event audit. */
@Repository
public class PaymentT0ManualAuditStore {
    private final JdbcTemplate jdbc;

    public PaymentT0ManualAuditStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    public void record(UUID paymentId, String actor, UUID correlationId,
            String outcome, Instant at) {
        jdbc.update("""
                INSERT INTO payment_t0_manual_reconciliation_audit
                    (event_id, payment_id, actor_subject, correlation_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), paymentId, actor, correlationId, outcome,
                Timestamp.from(at));
    }
}

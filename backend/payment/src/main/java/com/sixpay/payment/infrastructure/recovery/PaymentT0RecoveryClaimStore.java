package com.sixpay.payment.infrastructure.recovery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Durable claims are short transactions; no Core Banking HTTP call is performed here. */
@Repository
public class PaymentT0RecoveryClaimStore {
    public record Claim(UUID paymentId, UUID token, int attempt) { }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public PaymentT0RecoveryClaimStore(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transactions = new TransactionTemplate(Objects.requireNonNull(txManager));
    }

    public List<Claim> claim(Instant now, PaymentT0RecoveryBatchProperties props) {
        Objects.requireNonNull(now);
        props.validate();
        List<Claim> claims = transactions.execute(status -> {
            Instant from = now.minus(props.getWindow());
            Instant cutoff = now.minus(props.getMinimumAge());
            // The original posting authorization audit event, not mutable updated_at,
            // provides the stable eligibility clock. Do not reuse outbox delivery claims.
            jdbc.update("""
                INSERT INTO payment_t0_recovery_attempts
                    (payment_id, first_posting_at, next_attempt_at)
                SELECT p.payment_id, MIN(a.occurred_at), MIN(a.occurred_at) + (? * INTERVAL '1 second')
                FROM payments p
                JOIN payment_audit a ON a.payment_id = p.payment_id
                LEFT JOIN payment_t0_recovery_attempts r ON r.payment_id = p.payment_id
                WHERE r.payment_id IS NULL
                  AND p.status IN ('POSTING_PENDING', 'POSTING_OUTCOME_UNKNOWN')
                  AND a.event_type = 'PaymentPostingAuthorized'
                  AND a.occurred_at >= ? AND a.occurred_at <= ?
                GROUP BY p.payment_id
                ORDER BY MIN(a.occurred_at), p.payment_id
                LIMIT ?
                ON CONFLICT (payment_id) DO NOTHING
                """, ps -> {
                    ps.setLong(1, props.getMinimumAge().getSeconds());
                    ps.setTimestamp(2, Timestamp.from(from));
                    ps.setTimestamp(3, Timestamp.from(cutoff));
                    ps.setInt(4, props.getBatchSize());
                });

            List<UUID> ids = jdbc.query("""
                SELECT r.payment_id
                FROM payment_t0_recovery_attempts r
                JOIN payments p ON p.payment_id = r.payment_id
                WHERE p.status IN ('POSTING_PENDING', 'POSTING_OUTCOME_UNKNOWN')
                  AND r.resolved_at IS NULL
                  AND r.first_posting_at >= ? AND r.first_posting_at <= ?
                  AND r.next_attempt_at <= ?
                  AND (r.claim_token IS NULL OR r.claimed_at < ?)
                ORDER BY r.next_attempt_at, r.payment_id
                FOR UPDATE OF r SKIP LOCKED
                LIMIT ?
                """, ps -> {
                    ps.setTimestamp(1, Timestamp.from(from));
                    ps.setTimestamp(2, Timestamp.from(cutoff));
                    ps.setTimestamp(3, Timestamp.from(now));
                    ps.setTimestamp(4, Timestamp.from(now.minus(props.getClaimTimeout())));
                    ps.setInt(5, props.getBatchSize());
                }, (rs, row) -> (UUID) rs.getObject(1));

            List<Claim> acquired = new ArrayList<>(ids.size());
            for (UUID id : ids) {
                UUID token = UUID.randomUUID();
                int updated = jdbc.update("""
                    UPDATE payment_t0_recovery_attempts
                    SET claim_token = ?, claimed_at = ?, claimed_by = ?,
                        attempt_count = attempt_count + 1
                    WHERE payment_id = ? AND resolved_at IS NULL
                    """, ps -> {
                        ps.setObject(1, token);
                        ps.setTimestamp(2, Timestamp.from(now));
                        ps.setString(3, props.getWorkerId());
                        ps.setObject(4, id);
                    });
                if (updated != 1) {
                    throw new IllegalStateException("T0 claim changed during acquisition");
                }
                Integer attempt = jdbc.queryForObject(
                        "SELECT attempt_count FROM payment_t0_recovery_attempts WHERE payment_id = ?",
                        Integer.class, id);
                acquired.add(new Claim(id, token, Objects.requireNonNull(attempt)));
            }
            return acquired;
        });
        return claims == null ? List.of() : List.copyOf(claims);
    }

    /** Operator and R2 worker compete for the same durable row/lease. */
    public java.util.Optional<Claim> claimSelected(UUID paymentId, Instant now,
            PaymentT0RecoveryBatchProperties props, String owner) {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(now);
        Objects.requireNonNull(owner);
        if (owner.isBlank() || owner.length() > 150) {
            throw new IllegalArgumentException("Invalid claim owner");
        }
        props.validate();
        return transactions.execute(status -> {
            Instant from = now.minus(props.getWindow());
            Instant cutoff = now.minus(props.getMinimumAge());
            jdbc.update("""
                INSERT INTO payment_t0_recovery_attempts
                    (payment_id, first_posting_at, next_attempt_at)
                SELECT p.payment_id, MIN(a.occurred_at),
                    MIN(a.occurred_at) + (? * INTERVAL '1 second')
                FROM payments p
                JOIN payment_audit a ON a.payment_id = p.payment_id
                WHERE p.payment_id = ?
                  AND p.status IN ('POSTING_PENDING', 'POSTING_OUTCOME_UNKNOWN')
                  AND a.event_type = 'PaymentPostingAuthorized'
                  AND a.occurred_at >= ? AND a.occurred_at <= ?
                GROUP BY p.payment_id
                ON CONFLICT (payment_id) DO NOTHING
                """, ps -> {
                    ps.setLong(1, props.getMinimumAge().getSeconds());
                    ps.setObject(2, paymentId);
                    ps.setTimestamp(3, Timestamp.from(from));
                    ps.setTimestamp(4, Timestamp.from(cutoff));
                });
            List<UUID> locked = jdbc.query("""
                SELECT r.payment_id
                FROM payment_t0_recovery_attempts r
                JOIN payments p ON p.payment_id = r.payment_id
                WHERE r.payment_id = ?
                  AND p.status IN ('POSTING_PENDING', 'POSTING_OUTCOME_UNKNOWN')
                  AND r.resolved_at IS NULL
                  AND r.first_posting_at >= ? AND r.first_posting_at <= ?
                  AND r.next_attempt_at <= ?
                  AND (r.claim_token IS NULL OR r.claimed_at < ?)
                FOR UPDATE OF r SKIP LOCKED
                """, ps -> {
                    ps.setObject(1, paymentId);
                    ps.setTimestamp(2, Timestamp.from(from));
                    ps.setTimestamp(3, Timestamp.from(cutoff));
                    ps.setTimestamp(4, Timestamp.from(now));
                    ps.setTimestamp(5, Timestamp.from(now.minus(props.getClaimTimeout())));
                }, (rs, row) -> (UUID) rs.getObject(1));
            if (locked.isEmpty()) {
                return java.util.Optional.<Claim>empty();
            }
            UUID token = UUID.randomUUID();
            int updated = jdbc.update("""
                UPDATE payment_t0_recovery_attempts
                SET claim_token = ?, claimed_at = ?, claimed_by = ?,
                    attempt_count = attempt_count + 1
                WHERE payment_id = ? AND resolved_at IS NULL
                """, ps -> {
                    ps.setObject(1, token);
                    ps.setTimestamp(2, Timestamp.from(now));
                    ps.setString(3, owner);
                    ps.setObject(4, paymentId);
                });
            if (updated != 1) {
                throw new IllegalStateException("T0 selected claim changed during acquisition");
            }
            Integer attempt = jdbc.queryForObject(
                    "SELECT attempt_count FROM payment_t0_recovery_attempts WHERE payment_id = ?",
                    Integer.class, paymentId);
            return java.util.Optional.of(new Claim(paymentId, token,
                    Objects.requireNonNull(attempt)));
        });
    }
    /** Fence a late worker: only the current token can close the attempt. */
    public boolean completed(Claim claim, Instant now) {
        return transition(claim, now, null, true);
    }

    public boolean retry(Claim claim, Instant now, Duration backoff) {
        Objects.requireNonNull(backoff);
        if (backoff.isNegative() || backoff.isZero()) {
            throw new IllegalArgumentException("T0 retry backoff must be positive");
        }
        return transition(claim, now, backoff, false);
    }

    private boolean transition(Claim claim, Instant now, Duration backoff, boolean resolved) {
        Objects.requireNonNull(claim);
        Objects.requireNonNull(now);
        Integer rows = transactions.execute(status -> jdbc.update("""
            UPDATE payment_t0_recovery_attempts
            SET claim_token = NULL, claimed_at = NULL, claimed_by = NULL,
                resolved_at = ?, next_attempt_at = ?
            WHERE payment_id = ? AND claim_token = ? AND attempt_count = ?
              AND resolved_at IS NULL
            """, ps -> {
                ps.setTimestamp(1, resolved ? Timestamp.from(now) : null);
                ps.setTimestamp(2, Timestamp.from(resolved ? now : now.plus(backoff)));
                ps.setObject(3, claim.paymentId());
                ps.setObject(4, claim.token());
                ps.setInt(5, claim.attempt());
            }));
        return rows != null && rows == 1;
    }
}

package com.sixpay.payment.infrastructure.recovery;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL claim, fencing, lease expiry, and rolling-window regression. */
@Testcontainers(disabledWithoutDocker = true)
class PaymentT0RecoveryClaimStoreIT {
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    private JdbcTemplate jdbc;
    private PaymentT0RecoveryClaimStore store;

    @BeforeEach
    void setup() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()).locations("classpath:db/migration").load().migrate();
        var ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);
        store = new PaymentT0RecoveryClaimStore(jdbc, new DataSourceTransactionManager(ds));
        jdbc.execute("TRUNCATE TABLE payment_t0_recovery_attempts, payment_outbox_events, payment_audit, payments RESTART IDENTITY CASCADE");
    }

    @Test
    void twoConcurrentWorkersClaimOnlyOnce() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(5)), "POSTING_OUTCOME_UNKNOWN");
        var ready = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { ready.await(); return store.claim(NOW, props("worker-a")); });
            var b = pool.submit(() -> { ready.await(); return store.claim(NOW, props("worker-b")); });
            ready.countDown();
            var results = new java.util.ArrayList<PaymentT0RecoveryClaimStore.Claim>();
            results.addAll(a.get());
            results.addAll(b.get());
            assertEquals(1, results.size());
            assertEquals(paymentId, results.getFirst().paymentId());
        }
    }

    @Test
    void staleLeaseReclaimedAndOldTokenCannotComplete() {
        UUID paymentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(5)), "POSTING_PENDING");
        var old = store.claim(NOW, props("worker-a")).getFirst();
        assertTrue(store.claim(NOW.plus(Duration.ofMinutes(1)), props("worker-b")).isEmpty());
        var next = store.claim(NOW.plus(Duration.ofMinutes(6)), props("worker-b")).getFirst();
        assertEquals(2, next.attempt());
        assertNotEquals(old.token(), next.token());
        assertFalse(store.completed(old, NOW.plus(Duration.ofMinutes(6))));
        assertTrue(store.retry(next, NOW.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5)));
        assertTrue(store.claim(NOW.plus(Duration.ofMinutes(7)), props("worker-b")).isEmpty());
        assertEquals(3, store.claim(NOW.plus(Duration.ofMinutes(12)), props("worker-c")).getFirst().attempt());
    }

    @Test
    void windowExcludesOldPostingEvenWhenRecentlyUpdated() {
        UUID oldId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID freshId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        insertPayment(oldId, NOW.minus(Duration.ofHours(49)), "POSTING_OUTCOME_UNKNOWN");
        insertPayment(freshId, NOW.minus(Duration.ofMinutes(1)), "POSTING_OUTCOME_UNKNOWN");
        assertTrue(store.claim(NOW, props("worker-a")).isEmpty());
        assertEquals(List.of(freshId), store.claim(NOW.plus(Duration.ofMinutes(2)),
                props("worker-a")).stream().map(PaymentT0RecoveryClaimStore.Claim::paymentId).toList());
    }

    @Test
    void resolvedPaymentIsNeverReclaimed() {
        UUID paymentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(5)), "POSTING_OUTCOME_UNKNOWN");
        var claim = store.claim(NOW, props("worker-a")).getFirst();
        assertTrue(store.completed(claim, NOW));
        assertTrue(store.claim(NOW.plus(Duration.ofMinutes(10)), props("worker-b")).isEmpty());
    }

    @Test
    void manualClaimExcludesSchedulerAndHonoursFencing() {
        UUID paymentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(5)), "POSTING_OUTCOME_UNKNOWN");
        var manual = store.claimSelected(paymentId, NOW, props("manual"),
                "manual:ops-1").orElseThrow();
        assertTrue(store.claim(NOW, props("scheduler")).isEmpty());
        assertTrue(store.claimSelected(paymentId, NOW, props("manual-2"),
                "manual:ops-2").isEmpty());
        var recovered = store.claim(NOW.plus(Duration.ofMinutes(6)),
                props("scheduler")).getFirst();
        assertFalse(store.completed(manual, NOW.plus(Duration.ofMinutes(6))));
        assertTrue(store.completed(recovered, NOW.plus(Duration.ofMinutes(6))));
    }

    @Test
    void twoOperatorsCannotClaimTheSamePayment() throws Exception {
        UUID paymentId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(5)), "POSTING_OUTCOME_UNKNOWN");
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                start.await();
                return store.claimSelected(paymentId, NOW, props("a"), "manual:a");
            });
            var second = pool.submit(() -> {
                start.await();
                return store.claimSelected(paymentId, NOW, props("b"), "manual:b");
            });
            start.countDown();
            assertEquals(1, (first.get().isPresent() ? 1 : 0)
                    + (second.get().isPresent() ? 1 : 0));
        }
    }

    private static PaymentT0RecoveryBatchProperties props(String workerId) {
        var props = new PaymentT0RecoveryBatchProperties();
        props.setWorkerId(workerId);
        return props;
    }

    private void insertPayment(UUID id, Instant postingAt, String status) {
        String ref = "PAY-" + id.toString().replace("-", "").substring(0, 26).toUpperCase();
        Instant receivedAt = postingAt.minusSeconds(60);
        jdbc.update("""
            INSERT INTO payments (payment_id, public_payment_reference, payment_source,
                external_payment_reference, external_subscription_reference,
                requested_amount, requested_currency, status, business_version,
                received_at, updated_at, state_payload, persistence_version)
            VALUES (?, ?, 'TRESOR_PAY', ?, 'SUB-TEST', 1000, 'XAF', ?, 1, ?, ?, ?::jsonb, 0)
            """, id, ref, "EXT-" + id, status,
                Timestamp.from(receivedAt), Timestamp.from(NOW), "{\"schemaVersion\":2}");
        jdbc.update("""
            INSERT INTO payment_audit (event_id, payment_id, public_payment_reference,
                event_type, payment_status, business_version, event_sequence,
                correlation_id, occurred_at)
            VALUES (?, ?, ?, 'PaymentPostingAuthorized', 'POSTING_PENDING', 1, 1, ?, ?)
            """, UUID.randomUUID(), id, ref, UUID.randomUUID().toString(), Timestamp.from(postingAt));
    }
    // BEGIN SIXPAY_R6_T0_VALIDATION_TESTS
    @Test
    void r6ManualSelectionRejectsOldPostingDespiteRecentPaymentUpdate() {
        UUID paymentId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        insertPayment(paymentId, NOW.minus(Duration.ofHours(49)), "POSTING_OUTCOME_UNKNOWN");
        // insertPayment stores payments.updated_at = NOW; the original posting audit wins.
        assertTrue(store.claimSelected(paymentId, NOW, props("manual"),
                "manual:r6-admin").isEmpty());
        Integer attempts = jdbc.queryForObject(
                "SELECT count(*) FROM payment_t0_recovery_attempts WHERE payment_id = ?",
                Integer.class, paymentId);
        assertEquals(0, attempts);
    }

    @Test
    void r6ManualSelectionRejectsPaymentBeforeMinimumPostingAge() {
        UUID paymentId = UUID.fromString("44444444-4444-4444-8444-444444444444");
        insertPayment(paymentId, NOW.minus(Duration.ofMinutes(1)), "POSTING_PENDING");
        assertTrue(store.claimSelected(paymentId, NOW, props("manual"),
                "manual:r6-admin").isEmpty());
    }
    // END SIXPAY_R6_T0_VALIDATION_TESTS

}

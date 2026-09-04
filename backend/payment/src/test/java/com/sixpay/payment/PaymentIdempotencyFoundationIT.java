package com.sixpay.payment;

import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyConcurrencyCoordinator;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyConflictException;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyDecision;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyHasher;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyReplayStore;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes =
                PaymentIdempotencyFoundationIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentIdempotencyFoundationIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            );

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentIdempotencyHasher hasher;

    @Autowired
    private PaymentIdempotencyReplayStore replayStore;

    @Autowired
    private PaymentIdempotencyConcurrencyCoordinator coordinator;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void replaysCompletedPaymentResult()
            throws Exception {

        String operation = "PAYMENT_CREATE";
        String key = "replay-" + UUID.randomUUID();

        String requestHash = hasher.hash(
                "{\"amount\":\"1000.00\",\"currency\":\"XAF\"}"
        );

        UUID paymentId = UUID.randomUUID();
        String paymentReference =
                publicReference(paymentId);

        inTransaction(() ->
                coordinator.executeLocked(
                        operation,
                        key,
                        () -> {
                            insertPayment(
                                    paymentId,
                                    paymentReference
                            );

                            PaymentIdempotencyDecision decision =
                                    replayStore.begin(
                                            operation,
                                            key,
                                            requestHash,
                                            now()
                                    );

                            assertThat(decision.kind())
                                    .isEqualTo(
                                            PaymentIdempotencyDecision
                                                    .Kind.NEW
                                    );

                            replayStore.complete(
                                    operation,
                                    key,
                                    requestHash,
                                    paymentId,
                                    "ACCEPTED",
                                    "{\"result\":\"accepted\"}",
                                    now().plusSeconds(1)
                            );

                            return null;
                        }
                )
        );

        PaymentIdempotencyDecision replay =
                inTransactionWithResult(() ->
                        coordinator.executeLocked(
                                operation,
                                key,
                                () -> replayStore.begin(
                                        operation,
                                        key,
                                        requestHash,
                                        now().plusSeconds(2)
                                )
                        )
                );

        assertThat(replay.kind())
                .isEqualTo(
                        PaymentIdempotencyDecision.Kind.REPLAY
                );

        assertThat(replay.paymentId())
                .isEqualTo(paymentId);

        assertThat(replay.responseStatus())
                .isEqualTo("ACCEPTED");

        JSONAssert.assertEquals(
                "{\"result\":\"accepted\"}",
                replay.responsePayload(),
                JSONCompareMode.STRICT
        );
    }

    @Test
    void rejectsSameKeyWithDifferentRequest() {

        String operation = "PAYMENT_CREATE";
        String key = "conflict-" + UUID.randomUUID();

        String originalHash = hasher.hash(
                "{\"amount\":\"1000.00\"}"
        );

        String conflictingHash = hasher.hash(
                "{\"amount\":\"2000.00\"}"
        );

        inTransaction(() ->
                coordinator.executeLocked(
                        operation,
                        key,
                        () -> {
                            replayStore.begin(
                                    operation,
                                    key,
                                    originalHash,
                                    now()
                            );

                            return null;
                        }
                )
        );

        assertThatThrownBy(() ->
                inTransaction(() ->
                        coordinator.executeLocked(
                                operation,
                                key,
                                () -> {
                                    replayStore.begin(
                                            operation,
                                            key,
                                            conflictingHash,
                                            now().plusSeconds(1)
                                    );

                                    return null;
                                }
                        )
                )
        )
                .isInstanceOf(
                        PaymentIdempotencyConflictException.class
                )
                .hasMessageContaining(
                        "Idempotency key conflict"
                )
                .hasMessageContaining(operation)
                .hasMessageContaining(key);
    }


    @Test
    void preservesUnknownOutcomeUntilAuthoritativeRecovery() {
        String operation = "PAYMENT_CONFIRMATION_CREATE";
        String key = "unknown-" + UUID.randomUUID();
        String requestHash = hasher.hash(
                "{\"paymentReference\":\"PAY-UNKNOWN\"}"
        );

        UUID paymentId = UUID.randomUUID();
        String paymentReference = publicReference(paymentId);
        Instant unknownAt = now().plusSeconds(1);

        inTransaction(() ->
                coordinator.executeLocked(
                        operation,
                        key,
                        () -> {
                            insertPayment(
                                    paymentId,
                                    paymentReference
                            );

                            PaymentIdempotencyDecision started =
                                    replayStore.begin(
                                            operation,
                                            key,
                                            requestHash,
                                            now()
                                    );

                            assertThat(started.kind())
                                    .isEqualTo(
                                            PaymentIdempotencyDecision.Kind.NEW
                                    );

                            replayStore.markOutcomeUnknown(
                                    operation,
                                    key,
                                    requestHash,
                                    paymentId,
                                    null,
                                    "timeout after request dispatch",
                                    unknownAt
                            );
                            return null;
                        }
                )
        );

        PaymentIdempotencyDecision unknown =
                inTransactionWithResult(() ->
                        coordinator.executeLocked(
                                operation,
                                key,
                                () -> replayStore.begin(
                                        operation,
                                        key,
                                        requestHash,
                                        now().plusSeconds(2)
                                )
                        )
                );

        assertThat(unknown.kind())
                .isEqualTo(
                        PaymentIdempotencyDecision.Kind.OUTCOME_UNKNOWN
                );
        assertThat(unknown.paymentId()).isEqualTo(paymentId);
        assertThat(unknown.recoveryReference()).isNull();
        assertThat(unknown.recoveryReason())
                .isEqualTo("timeout after request dispatch");
        assertThat(unknown.unknownOutcomeAt()).isEqualTo(unknownAt);

        inTransaction(() ->
                coordinator.executeLocked(
                        operation,
                        key,
                        () -> {
                            replayStore.complete(
                                    operation,
                                    key,
                                    requestHash,
                                    paymentId,
                                    "CHALLENGE_ACTIVE",
                                    "{\"challenge\":\"recovered\"}",
                                    now().plusSeconds(3)
                            );
                            return null;
                        }
                )
        );

        PaymentIdempotencyDecision replay =
                inTransactionWithResult(() ->
                        coordinator.executeLocked(
                                operation,
                                key,
                                () -> replayStore.begin(
                                        operation,
                                        key,
                                        requestHash,
                                        now().plusSeconds(4)
                                )
                        )
                );

        assertThat(replay.kind())
                .isEqualTo(PaymentIdempotencyDecision.Kind.REPLAY);
        assertThat(replay.responseStatus())
                .isEqualTo("CHALLENGE_ACTIVE");
    }

    @Test
    void serializesConcurrentTransactionsForSameKey()
            throws Exception {
        String operation = "PAYMENT_CREATE";
        String key = "concurrent-" + UUID.randomUUID();

        AtomicInteger insideCriticalSection =
                new AtomicInteger();
        AtomicInteger maximumConcurrency =
                new AtomicInteger();
        AtomicInteger executions =
                new AtomicInteger();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() ->
                    runLockedProbe(
                            operation,
                            key,
                            ready,
                            start,
                            insideCriticalSection,
                            maximumConcurrency,
                            executions
                    )
            );
            Future<?> second = executor.submit(() ->
                    runLockedProbe(
                            operation,
                            key,
                            ready,
                            start,
                            insideCriticalSection,
                            maximumConcurrency,
                            executions
                    )
            );

            assertThat(
                    ready.await(5, TimeUnit.SECONDS)
            ).isTrue();

            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }

        assertThat(executions.get()).isEqualTo(2);
        assertThat(maximumConcurrency.get()).isOne();
    }

    private void runLockedProbe(
            String operation,
            String key,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger inside,
            AtomicInteger maximum,
            AtomicInteger executions
    ) {
        ready.countDown();
        await(start);

        inTransaction(() ->
                coordinator.executeLocked(
                        operation,
                        key,
                        () -> {
                            int current =
                                    inside.incrementAndGet();
                            maximum.accumulateAndGet(
                                    current,
                                    Math::max
                            );
                            executions.incrementAndGet();

                            try {
                                Thread.sleep(250);
                            } catch (
                                    InterruptedException exception
                            ) {
                                Thread.currentThread()
                                        .interrupt();
                                throw new IllegalStateException(
                                        exception
                                );
                            } finally {
                                inside.decrementAndGet();
                            }
                            return null;
                        }
                )
        );
    }

    private void insertPayment(
            UUID paymentId,
            String paymentReference
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO payments (
                    payment_id,
                    public_payment_reference,
                    payment_source,
                    external_payment_reference,
                    external_subscription_reference,
                    financial_institution_code,
                    requested_amount,
                    requested_currency,
                    status,
                    business_version,
                    received_at,
                    updated_at,
                    finalized_at,
                    state_payload,
                    persistence_version
                ) VALUES (
                    ?,
                    ?,
                    'TRESOR_PAY',
                    ?,
                    ?,
                    'SIXPAY_BANK',
                    1000.00,
                    'XAF',
                    'RECEIVED',
                    1,
                    TIMESTAMPTZ '2026-08-01 19:00:00+00',
                    TIMESTAMPTZ '2026-08-01 19:00:00+00',
                    NULL,
                    '{"schemaVersion":1}'::jsonb,
                    0
                )
                """,
                paymentId,
                paymentReference,
                "EXT-" + paymentId,
                "SUB-" + paymentId
        );
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> action.run());
    }

    private <T> T inTransactionWithResult(
            java.util.function.Supplier<T> action
    ) {
        return new TransactionTemplate(transactionManager)
                .execute(status -> action.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Concurrency test latch timed output"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static Instant now() {
        return Instant.parse("2026-08-01T19:00:00Z");
    }

    private static String publicReference(UUID paymentId) {
        return "PAY-" + paymentId
                .toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(
            PaymentModuleConfiguration.class
    )
    static class TestApplication {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () -> Optional.<AuthenticatedUser>empty();
        }
    }
}

package com.sixpay.payment.performance;

import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyConcurrencyCoordinator;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyDecision;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyHasher;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyReplayStore;
import com.sixpay.payment.infrastructure.persistence.PaymentJpaEntity;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@SpringBootTest(
        classes =
                PaymentConcurrencyPerformanceIT
                        .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PaymentConcurrencyPerformanceIT {

    private static final Instant BASE_TIME =
            Instant.parse("2026-08-01T20:30:00Z");

    private static final int PAYMENT_VOLUME =
            Integer.getInteger(
                    "sixpay.payment.performance.volume",
                    3_000
            );

    private static final int CONCURRENT_REPLAYS =
            Integer.getInteger(
                    "sixpay.payment.performance.replays",
                    128
            );

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
        registry.add(
                "spring.datasource.hikari.maximum-pool-size",
                () -> "20"
        );
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PaymentIdempotencyHasher hasher;

    @Autowired
    private PaymentIdempotencyReplayStore replayStore;

    @Autowired
    private PaymentIdempotencyConcurrencyCoordinator coordinator;

    @Test
    void optimisticLockRejectsSecondConcurrentCommit() {
        UUID paymentId = UUID.randomUUID();
        insertPayment(
                paymentId,
                reference(paymentId),
                "LOCK-" + paymentId
        );

        EntityManager first =
                entityManagerFactory.createEntityManager();
        EntityManager second =
                entityManagerFactory.createEntityManager();

        try {
            first.getTransaction().begin();
            second.getTransaction().begin();

            PaymentJpaEntity firstCopy =
                    first.find(
                            PaymentJpaEntity.class,
                            paymentId
                    );
            PaymentJpaEntity secondCopy =
                    second.find(
                            PaymentJpaEntity.class,
                            paymentId
                    );

            mutateEntity(
                    firstCopy,
                    PaymentStatus.AUTHORIZATION_CHECKING,
                    2L,
                    BASE_TIME.plusSeconds(1)
            );
            mutateEntity(
                    secondCopy,
                    PaymentStatus.BANKING_VERIFICATION_PENDING,
                    2L,
                    BASE_TIME.plusSeconds(2)
            );

            first.getTransaction().commit();

            assertThatThrownBy(
                    () -> second.getTransaction().commit()
            )
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(
                            OptimisticLockException.class
                    );

            assertThat(
                    jdbc.queryForObject(
                            """
                            SELECT persistence_version
                              FROM payments
                             WHERE payment_id = ?
                            """,
                            Long.class,
                            paymentId
                    )
            ).isEqualTo(1L);

            assertThat(
                    jdbc.queryForObject(
                            """
                            SELECT status
                              FROM payments
                             WHERE payment_id = ?
                            """,
                            String.class,
                            paymentId
                    )
            ).isEqualTo(
                    PaymentStatus
                            .AUTHORIZATION_CHECKING
                            .name()
            );
        } finally {
            rollbackIfActive(first);
            rollbackIfActive(second);
            first.close();
            second.close();
        }
    }

    @Test
    void serializesStrongConcurrencyAndReplaysOneResult() {
        assertTimeoutPreemptively(
                Duration.ofSeconds(60),
                () -> {
                    String operation = "PAYMENT_CREATE";
                    String key =
                            "load-replay-" + UUID.randomUUID();
                    String requestHash = hasher.hash(
                            """
                            {
                              "amount": "1000.00",
                              "currency": "XAF"
                            }
                            """
                    );
                    UUID paymentId = UUID.randomUUID();

                    insertPayment(
                            paymentId,
                            reference(paymentId),
                            "REPLAY-" + paymentId
                    );

                    CountDownLatch ready =
                            new CountDownLatch(
                                    CONCURRENT_REPLAYS
                            );
                    CountDownLatch start =
                            new CountDownLatch(1);

                    List<Future<
                            PaymentIdempotencyDecision.Kind
                            >> futures =
                            new ArrayList<>(
                                    CONCURRENT_REPLAYS
                            );

                    try (var executor =
                                 Executors
                                         .newVirtualThreadPerTaskExecutor()) {

                        for (int index = 0;
                             index < CONCURRENT_REPLAYS;
                             index++) {
                            futures.add(
                                    executor.submit(() -> {
                                        ready.countDown();

                                        if (!start.await(
                                                10,
                                                TimeUnit.SECONDS
                                        )) {
                                            throw new IllegalStateException(
                                                    "Replay start timed output"
                                            );
                                        }

                                        return new TransactionTemplate(
                                                transactionManager
                                        ).execute(status ->
                                                coordinator.executeLocked(
                                                        operation,
                                                        key,
                                                        () -> {
                                                            var decision =
                                                                    replayStore
                                                                            .begin(
                                                                                    operation,
                                                                                    key,
                                                                                    requestHash,
                                                                                    BASE_TIME
                                                                            );

                                                            if (decision.kind()
                                                                    == PaymentIdempotencyDecision
                                                                    .Kind.NEW) {
                                                                replayStore
                                                                        .complete(
                                                                                operation,
                                                                                key,
                                                                                requestHash,
                                                                                paymentId,
                                                                                "ACCEPTED",
                                                                                """
                                                                                {
                                                                                  "paymentReference": "%s"
                                                                                }
                                                                                """.formatted(
                                                                                        reference(
                                                                                                paymentId
                                                                                        )
                                                                                ),
                                                                                BASE_TIME
                                                                                        .plusSeconds(
                                                                                                1
                                                                                        )
                                                                        );
                                                            }

                                                            return decision
                                                                    .kind();
                                                        }
                                                )
                                        );
                                    })
                            );
                        }

                        assertThat(
                                ready.await(
                                        10,
                                        TimeUnit.SECONDS
                                )
                        ).isTrue();

                        start.countDown();

                        List<
                                PaymentIdempotencyDecision.Kind
                                > decisions =
                                new ArrayList<>(
                                        CONCURRENT_REPLAYS
                                );

                        for (Future<
                                PaymentIdempotencyDecision.Kind
                                > future : futures) {
                            decisions.add(
                                    future.get(
                                            45,
                                            TimeUnit.SECONDS
                                    )
                            );
                        }

                        assertThat(
                                decisions.stream()
                                        .filter(kind ->
                                                kind
                                                        == PaymentIdempotencyDecision
                                                        .Kind.NEW
                                        )
                                        .count()
                        ).isEqualTo(1L);

                        assertThat(
                                decisions.stream()
                                        .filter(kind ->
                                                kind
                                                        == PaymentIdempotencyDecision
                                                        .Kind.REPLAY
                                        )
                                        .count()
                        ).isEqualTo(
                                CONCURRENT_REPLAYS - 1L
                        );
                    }

                    var replay = replayStore.findReplay(
                            operation,
                            key,
                            requestHash
                    );

                    assertThat(replay)
                            .isPresent()
                            .get()
                            .extracting(
                                    PaymentIdempotencyDecision
                                            ::kind
                            )
                            .isEqualTo(
                                    PaymentIdempotencyDecision
                                            .Kind.REPLAY
                            );

                    assertThat(
                            jdbc.queryForObject(
                                    """
                                    SELECT COUNT(*)
                                      FROM payment_idempotency
                                     WHERE operation = ?
                                       AND idempotency_key = ?
                                    """,
                                    Long.class,
                                    operation,
                                    key
                            )
                    ).isEqualTo(1L);
                }
        );
    }

    @Test
    void persistsThousandsOfPaymentsWithVirtualThreadWorkers() {
        assertTimeoutPreemptively(
                Duration.ofSeconds(90),
                () -> {
                    String runId = UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

                    int workerCount = Math.min(
                            30,
                            Math.max(
                                    10,
                                    PAYMENT_VOLUME / 100
                            )
                    );

                    int chunkSize =
                            (PAYMENT_VOLUME + workerCount - 1)
                                    / workerCount;

                    CountDownLatch start =
                            new CountDownLatch(1);

                    List<Future<Integer>> futures =
                            new ArrayList<>(workerCount);

                    try (var executor =
                                 Executors
                                         .newVirtualThreadPerTaskExecutor()) {

                        for (int worker = 0;
                             worker < workerCount;
                             worker++) {

                            int from = worker * chunkSize;
                            int to = Math.min(
                                    PAYMENT_VOLUME,
                                    from + chunkSize
                            );

                            if (from >= to) {
                                continue;
                            }

                            futures.add(
                                    executor.submit(() -> {
                                        assertThat(
                                                Thread.currentThread()
                                                        .isVirtual()
                                        ).isTrue();

                                        if (!start.await(
                                                10,
                                                TimeUnit.SECONDS
                                        )) {
                                            throw new IllegalStateException(
                                                    "Volume start timed output"
                                            );
                                        }

                                        return new TransactionTemplate(
                                                transactionManager
                                        ).execute(status -> {
                                            List<Object[]> batch =
                                                    new ArrayList<>(
                                                            to - from
                                                    );

                                            for (int index = from;
                                                 index < to;
                                                 index++) {
                                                batch.add(
                                                        paymentArguments(
                                                                runId,
                                                                index
                                                        )
                                                );
                                            }

                                            jdbc.batchUpdate(
                                                    insertSql(),
                                                    batch
                                            );

                                            return batch.size();
                                        });
                                    })
                            );
                        }

                        start.countDown();

                        int inserted = 0;
                        for (Future<Integer> future : futures) {
                            inserted += future.get(
                                    75,
                                    TimeUnit.SECONDS
                            );
                        }

                        assertThat(inserted)
                                .isEqualTo(PAYMENT_VOLUME);
                    }

                    assertThat(
                            jdbc.queryForObject(
                                    """
                                    SELECT COUNT(*)
                                      FROM payments
                                     WHERE external_payment_reference
                                           LIKE ?
                                    """,
                                    Long.class,
                                    "PERF-" + runId + "-%"
                            )
                    ).isEqualTo((long) PAYMENT_VOLUME);

                    assertThat(
                            jdbc.queryForObject(
                                    """
                                    SELECT COUNT(DISTINCT payment_id)
                                      FROM payments
                                     WHERE external_payment_reference
                                           LIKE ?
                                    """,
                                    Long.class,
                                    "PERF-" + runId + "-%"
                            )
                    ).isEqualTo((long) PAYMENT_VOLUME);
                }
        );
    }

    private Object[] paymentArguments(
            String runId,
            int index
    ) {
        UUID paymentId = UUID.nameUUIDFromBytes(
                (runId + ":" + index)
                        .getBytes(StandardCharsets.UTF_8)
        );

        Instant timestamp =
                BASE_TIME.plusMillis(index);

        return new Object[]{
                paymentId,
                reference(paymentId),
                "PERF-" + runId + "-" + index,
                "SUB-" + runId + "-" + index,
                sqlTimestamp(timestamp),
                sqlTimestamp(timestamp)
        };
    }

    private static java.time.OffsetDateTime sqlTimestamp(
            Instant instant
    ) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static String insertSql() {
        return """
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
                    ?,
                    ?,
                    NULL,
                    '{"schemaVersion":1}'::jsonb,
                    0
                )
                """;
    }

    private void insertPayment(
            UUID paymentId,
            String paymentReference,
            String externalReference
    ) {
        var timestamp =
                BASE_TIME.atOffset(ZoneOffset.UTC);

        jdbc.update(
                insertSql(),
                paymentId,
                paymentReference,
                externalReference,
                "SUB-" + paymentId,
                timestamp,
                timestamp
        );
    }

    private static void mutateEntity(
            PaymentJpaEntity entity,
            PaymentStatus status,
            long businessVersion,
            Instant updatedAt
    ) {
        setField(entity, "status", status);
        setField(
                entity,
                "businessVersion",
                businessVersion
        );
        setField(entity, "updatedAt", updatedAt);
        setField(
                entity,
                "statePayload",
                """
                {
                  "schemaVersion": 1,
                  "testMutation": "%s"
                }
                """.formatted(status.name())
        );
    }

    private static void setField(
            Object target,
            String fieldName,
            Object value
    ) {
        try {
            Field field = target.getClass()
                    .getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Cannot mutate test entity field "
                            + fieldName,
                    exception
            );
        }
    }

    private static void rollbackIfActive(
            EntityManager entityManager
    ) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private static String reference(UUID id) {
        return "PAY-" + id.toString()
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
            return Optional::<AuthenticatedUser>empty;
        }
    }
}

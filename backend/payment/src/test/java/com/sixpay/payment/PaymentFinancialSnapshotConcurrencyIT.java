package com.sixpay.payment;

import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.financial.*;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentFinancialSnapshotConcurrencyIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentFinancialSnapshotConcurrencyIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:15-alpine")
            );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentFinancialSnapshotRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentSameSnapshotProducesOneLogicalSnapshot()
            throws Exception {
        PaymentFinancialEventSnapshot snapshot = finalizedSnapshot(
                UUID.fromString("b806c2d1-1847-4a45-8f06-e050cf011001")
        );
        insertPayment(snapshot.paymentId().value());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(
                    () -> saveAfterBarrier(snapshot, ready, start)
            );
            Future<?> second = executor.submit(
                    () -> saveAfterBarrier(snapshot, ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            consumeAllowingConstraintRace(first);
            consumeAllowingConstraintRace(second);
        }

        PaymentFinancialEventSnapshot persisted =
                repository.findByPaymentId(snapshot.paymentId())
                        .orElseThrow();

        assertThat(persisted.sameSnapshot(snapshot)).isTrue();
    }

    @Test
    void concurrentDifferentSnapshotsNeverReplaceExistingSnapshot()
            throws Exception {
        UUID paymentUuid =
                UUID.fromString("b806c2d1-1847-4a45-8f06-e050cf011002");

        PaymentFinancialEventSnapshot firstSnapshot =
                finalizedSnapshot(
                        UUID.fromString("c906c2d1-1847-4a45-8f06-e050cf011001"),
                        paymentUuid,
                        "vault:creditor:0001"
                );
        PaymentFinancialEventSnapshot secondSnapshot =
                finalizedSnapshot(
                        UUID.fromString("c906c2d1-1847-4a45-8f06-e050cf011002"),
                        paymentUuid,
                        "vault:creditor:0002"
                );
        insertPayment(paymentUuid);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(
                    () -> saveAfterBarrier(firstSnapshot, ready, start)
            );
            Future<?> second = executor.submit(
                    () -> saveAfterBarrier(secondSnapshot, ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            consumeAllowingConstraintRace(first);
            consumeAllowingConstraintRace(second);
        }

        PaymentFinancialEventSnapshot persisted =
                repository.findByPaymentId(firstSnapshot.paymentId())
                        .orElseThrow();

        assertThat(
                persisted.sameSnapshot(firstSnapshot)
                        || persisted.sameSnapshot(secondSnapshot)
        ).isTrue();
    }

    private void saveAfterBarrier(
            PaymentFinancialEventSnapshot snapshot,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        await(start);
        repository.save(snapshot);
    }

    private static void consumeAllowingConstraintRace(
            Future<?> future
    ) throws Exception {
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException exception) {
            Throwable cause = exception.getCause();

            assertThat(cause)
                    .isInstanceOf(PaymentPersistenceException.class);

            assertThat(cause.getMessage())
                    .satisfiesAnyOf(
                            message -> assertThat(message)
                                    .contains("constraint violation"),
                            message -> assertThat(message)
                                    .contains("different financial snapshot")
                    );
        }
    }

    private void insertPayment(UUID paymentId) {
        String paymentReference =
                "PAY-" + paymentId
                        .toString()
                        .replace("-", "")
                        .substring(6)
                        .toUpperCase();

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
                    'LRB',
                    1000.00,
                    'XAF',
                    'RECEIVED',
                    1,
                    TIMESTAMPTZ '2026-09-06 18:00:00+00',
                    TIMESTAMPTZ '2026-09-06 18:00:00+00',
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

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for concurrency barrier"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static PaymentFinancialEventSnapshot finalizedSnapshot(
            UUID paymentUuid
    ) {
        return finalizedSnapshot(
                UUID.fromString("c906c2d1-1847-4a45-8f06-e050cf011000"),
                paymentUuid,
                "vault:creditor:0001"
        );
    }

    private static PaymentFinancialEventSnapshot finalizedSnapshot(
            UUID snapshotUuid,
            UUID paymentUuid,
            String creditor
    ) {
        Instant createdAt =
                Instant.parse("2026-09-06T18:00:00Z");
        Money amount =
                Money.of(new BigDecimal("1000.00"), "XAF");

        PaymentFinancialEventSnapshot snapshot =
                PaymentFinancialEventSnapshot.draft(
                        snapshotUuid,
                        new PaymentId(paymentUuid),
                        PublicPaymentReference.of(
                                "PAY-" + paymentUuid
                                        .toString()
                                        .replace("-", "")
                                        .substring(6)
                                        .toUpperCase()
                        ),
                        FinancialInstitutionCode.of("LRB"),
                        "vault:debtor:0001",
                        creditor,
                        amount,
                        "v1",
                        createdAt
                );

        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        UUID.randomUUID(),
                        1,
                        FinancialEntryDirection.DEBIT,
                        "vault:debtor:0001",
                        amount,
                        createdAt.plusSeconds(1)
                )
        );
        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        UUID.randomUUID(),
                        2,
                        FinancialEntryDirection.CREDIT,
                        creditor,
                        amount,
                        createdAt.plusSeconds(1)
                )
        );
        snapshot.finalizeAt(createdAt.plusSeconds(2));
        return snapshot;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(PaymentModuleConfiguration.class)
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        CurrentUserProvider currentUserProvider() {
            return () -> Optional.<AuthenticatedUser>empty();
        }
    }
}

package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes =
                PaymentOutboxClaimConcurrencyIntegrationTest
                        .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PaymentOutboxClaimConcurrencyIntegrationTest {

    private static final Instant BASE =
            Instant.parse("2026-08-04T18:00:00Z");

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("sixpay_payment")
                    .withUsername("sixpay")
                    .withPassword("sixpay");

    @DynamicPropertySource
    static void properties(
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
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.jpa.open-input-view",
                () -> false
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );

        registry.add(
                "spring.main.web-application-type",
                () -> "none"
        );
    }

    @Autowired
    private PaymentOutboxRepository repository;

    @Autowired
    private PaymentOutboxClaimService claimService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute(
                """
                TRUNCATE TABLE
                    payment_outbox_events,
                    payments
                RESTART IDENTITY CASCADE
                """
        );
    }

    @Test
    void twoWorkersNeverClaimTheSameEvent()
            throws Exception {

        repository.saveAndFlush(
                entity(
                        event("11111111"),
                        aggregate("aaaaaaaa"),
                        BASE
                )
        );

        CountDownLatch start =
                new CountDownLatch(1);

        try (var executor =
                     Executors.newFixedThreadPool(2)) {

            List<Callable<List<PaymentOutboxClaim>>> tasks =
                    List.of(
                            () -> {
                                start.await();

                                return claimService.claimAvailable(
                                        BASE.plusSeconds(1),
                                        BASE.minusSeconds(120),
                                        1,
                                        "worker-a"
                                );
                            },
                            () -> {
                                start.await();

                                return claimService.claimAvailable(
                                        BASE.plusSeconds(1),
                                        BASE.minusSeconds(120),
                                        1,
                                        "worker-b"
                                );
                            }
                    );

            var futures = tasks.stream()
                    .map(executor::submit)
                    .toList();

            start.countDown();

            List<PaymentOutboxClaim> allClaims =
                    futures.stream()
                            .flatMap(future -> {
                                try {
                                    return future.get().stream();
                                } catch (Exception exception) {
                                    throw new IllegalStateException(
                                            "Concurrent claim failed",
                                            exception
                                    );
                                }
                            })
                            .toList();

            assertEquals(
                    1,
                    allClaims.size()
            );

            assertEquals(
                    1,
                    allClaims.stream()
                            .map(PaymentOutboxClaim::eventId)
                            .distinct()
                            .count()
            );
        }

        PaymentOutboxEntity persisted =
                repository.findAll()
                        .getFirst();

        assertEquals(
                PaymentOutboxEntity.Status.PROCESSING,
                persisted.status()
        );

        assertEquals(
                1,
                persisted.attemptCount()
        );
    }

    @Test
    void differentAggregatesCanBeClaimedConcurrently()
            throws Exception {

        repository.saveAllAndFlush(
                List.of(
                        entity(
                                event("11111111"),
                                aggregate("aaaaaaaa"),
                                BASE
                        ),
                        entity(
                                event("22222222"),
                                aggregate("bbbbbbbb"),
                                BASE
                        )
                )
        );

        CountDownLatch start =
                new CountDownLatch(1);

        try (var executor =
                     Executors.newFixedThreadPool(2)) {

            List<Callable<List<PaymentOutboxClaim>>> tasks =
                    List.of(
                            () -> {
                                start.await();

                                return claimService.claimAvailable(
                                        BASE.plusSeconds(1),
                                        BASE.minusSeconds(120),
                                        1,
                                        "worker-a"
                                );
                            },
                            () -> {
                                start.await();

                                return claimService.claimAvailable(
                                        BASE.plusSeconds(1),
                                        BASE.minusSeconds(120),
                                        1,
                                        "worker-b"
                                );
                            }
                    );

            var futures = tasks.stream()
                    .map(executor::submit)
                    .toList();

            start.countDown();

            Set<UUID> claimedIds =
                    futures.stream()
                            .flatMap(future -> {
                                try {
                                    return future.get().stream();
                                } catch (Exception exception) {
                                    throw new IllegalStateException(
                                            "Concurrent claim failed",
                                            exception
                                    );
                                }
                            })
                            .map(PaymentOutboxClaim::eventId)
                            .collect(Collectors.toSet());

            assertEquals(
                    2,
                    claimedIds.size()
            );
        }
    }

    @Test
    void laterEventOfSameAggregateWaitsForItsPredecessor() {
        UUID aggregateId =
                aggregate("aaaaaaaa");

        PaymentOutboxEntity first = entity(
                event("11111111"),
                aggregateId,
                BASE
        );

        PaymentOutboxEntity second = entity(
                event("22222222"),
                aggregateId,
                BASE.plusSeconds(1)
        );

        repository.saveAllAndFlush(
                List.of(
                        first,
                        second
                )
        );

        List<PaymentOutboxClaim> firstBatch =
                claimService.claimAvailable(
                        BASE.plusSeconds(2),
                        BASE.minusSeconds(120),
                        10,
                        "worker-a"
                );

        assertEquals(
                1,
                firstBatch.size()
        );

        assertEquals(
                first.eventId(),
                firstBatch.getFirst().eventId()
        );

        new TransactionTemplate(
                transactionManager
        ).executeWithoutResult(status -> {
            PaymentOutboxEntity claimed =
                    repository.findById(
                            first.eventId()
                    ).orElseThrow();

            claimed.markPublished(
                    BASE.plusSeconds(3)
            );

            repository.flush();
        });

        List<PaymentOutboxClaim> secondBatch =
                claimService.claimAvailable(
                        BASE.plusSeconds(4),
                        BASE.minusSeconds(120),
                        10,
                        "worker-b"
                );

        assertEquals(
                1,
                secondBatch.size()
        );

        assertEquals(
                second.eventId(),
                secondBatch.getFirst().eventId()
        );
    }

    @Test
    void staleProcessingClaimIsRecoveredAndAttemptIncrements() {
        PaymentOutboxEntity outboxEvent = entity(
                event("11111111"),
                aggregate("aaaaaaaa"),
                BASE
        );

        repository.saveAndFlush(
                outboxEvent
        );

        List<PaymentOutboxClaim> firstClaims =
                claimService.claimAvailable(
                        BASE.plusSeconds(1),
                        BASE.minusSeconds(120),
                        1,
                        "worker-a"
                );

        assertEquals(
                1,
                firstClaims.size()
        );

        assertEquals(
                1,
                firstClaims.getFirst().attempt()
        );

        List<PaymentOutboxClaim> recoveredClaims =
                claimService.claimAvailable(
                        BASE.plusSeconds(301),
                        BASE.plusSeconds(2),
                        1,
                        "worker-b"
                );

        assertEquals(
                1,
                recoveredClaims.size()
        );

        assertEquals(
                2,
                recoveredClaims.getFirst().attempt()
        );

        assertEquals(
                "worker-b",
                recoveredClaims.getFirst().claimedBy()
        );

        PaymentOutboxEntity recovered =
                repository.findById(
                        outboxEvent.eventId()
                ).orElseThrow();

        assertEquals(
                PaymentOutboxEntity.Status.PROCESSING,
                recovered.status()
        );

        assertEquals(
                2,
                recovered.attemptCount()
        );

        assertEquals(
                "worker-b",
                recovered.claimedBy()
        );
    }

    @Test
    void terminalEventsAreNeverClaimed() {
        PaymentOutboxEntity published = entity(
                event("11111111"),
                aggregate("aaaaaaaa"),
                BASE
        );

        published.claim(
                BASE.plusSeconds(1),
                "worker-a"
        );

        published.markPublished(
                BASE.plusSeconds(2)
        );

        PaymentOutboxEntity dead = entity(
                event("22222222"),
                aggregate("bbbbbbbb"),
                BASE
        );

        dead.markDead(
                "invalid contract",
                BASE.plusSeconds(1)
        );

        repository.saveAllAndFlush(
                List.of(
                        published,
                        dead
                )
        );

        List<PaymentOutboxClaim> claims =
                claimService.claimAvailable(
                        BASE.plusSeconds(3),
                        BASE.minusSeconds(120),
                        10,
                        "worker-a"
                );

        assertTrue(
                claims.isEmpty()
        );
    }

    private PaymentOutboxEntity entity(
            UUID eventId,
            UUID aggregateId,
            Instant occurredAt
    ) {
        ensurePaymentExists(
                aggregateId
        );

        return PaymentOutboxEntity.create(
                eventId,
                aggregateId,
                "payment.observation-projection",
                1,
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                "{\"safe\":true}",
                occurredAt,
                occurredAt
        );
    }

    private void ensurePaymentExists(
            UUID paymentId
    ) {
        jdbc.update(
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
                    ?,
                    ?,
                    NULL,
                    '{"schemaVersion":2}'::jsonb,
                    0
                )
                ON CONFLICT (payment_id) DO NOTHING
                """,
                paymentId,
                paymentReference(paymentId),
                "EXT-" + paymentId,
                "SUB-" + paymentId,
                Timestamp.from(BASE),
                Timestamp.from(BASE)
        );
    }

    private static String paymentReference(
            UUID paymentId
    ) {
        return "PAY-"
                + paymentId.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();
    }

    private static UUID event(
            String prefix
    ) {
        return UUID.fromString(
                prefix
                        + "-1111-4111-8111-111111111111"
        );
    }

    private static UUID aggregate(
            String prefix
    ) {
        return UUID.fromString(
                prefix
                        + "-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SecurityTestConfiguration.class)
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityTestConfiguration {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return Optional::<AuthenticatedUser>empty;
        }
    }
}
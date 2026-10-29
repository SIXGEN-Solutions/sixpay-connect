package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.application.port.output.customer.PaymentCustomerSubscriptionPort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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


    @Test
    void genericAndCallbackClaimsHaveIndependentDurableProgress() {
        PaymentOutboxEntity entity = entity(event("55555555"), aggregate("eeeeeeee"), BASE);
        // The callback relay consumes the logical Payment domain-event stream.
        repository.saveAndFlush(entity);
        jdbc.update("UPDATE payment_outbox_events SET event_type = 'payment.domain-event' WHERE event_id = ?",
                entity.eventId());
        List<PaymentOutboxClaim> generic = claimService.claimAvailable(
                BASE.plusSeconds(1), BASE.minusSeconds(120), 10, "generic");
        assertEquals(1, generic.size());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            PaymentOutboxEntity claimed = repository.findById(entity.eventId()).orElseThrow();
            claimed.markPublished(BASE.plusSeconds(2));
            repository.flush();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            List<PaymentOutboxEntity> callback = repository.lockCallbackClaimable(
                    BASE.plusSeconds(3), BASE.minusSeconds(120), 10);
            assertEquals(1, callback.size());
            callback.getFirst().claimCallback(BASE.plusSeconds(3), "callback");
            repository.flush();
        });
        PaymentOutboxEntity persisted = repository.findById(entity.eventId()).orElseThrow();
        assertEquals(PaymentOutboxEntity.Status.PUBLISHED, persisted.status());
        assertEquals(PaymentOutboxEntity.Status.PROCESSING, persisted.callbackStatus());
    }

    @Test
    void callbackPublicationDoesNotConsumeGenericDelivery() {
        PaymentOutboxEntity entity = entity(event("66666666"), aggregate("ffffffff"), BASE);
        repository.saveAndFlush(entity);
        jdbc.update("UPDATE payment_outbox_events SET event_type = 'payment.domain-event' WHERE event_id = ?",
                entity.eventId());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var callback = repository.lockCallbackClaimable(
                    BASE.plusSeconds(1), BASE.minusSeconds(120), 10);
            assertEquals(1, callback.size());
            callback.getFirst().claimCallback(BASE.plusSeconds(1), "callback");
            callback.getFirst().markCallbackPublished(BASE.plusSeconds(2));
            repository.flush();
        });
        var generic = claimService.claimAvailable(BASE.plusSeconds(3), BASE.minusSeconds(120),
                10, "generic");
        assertEquals(1, generic.size());
        var persisted = repository.findById(entity.eventId()).orElseThrow();
        assertEquals(PaymentOutboxEntity.Status.PROCESSING, persisted.status());
        assertEquals(PaymentOutboxEntity.Status.PUBLISHED, persisted.callbackStatus());
    }

    @Test
    void callbackFailureRetriesWithoutAlteringGenericProgress() {
        PaymentOutboxEntity entity = entity(event("77777777"), aggregate("bbbbbbbb"), BASE);
        repository.saveAndFlush(entity);
        jdbc.update("UPDATE payment_outbox_events SET event_type = 'payment.domain-event' WHERE event_id = ?",
                entity.eventId());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var callback = repository.lockCallbackClaimable(
                    BASE.plusSeconds(1), BASE.minusSeconds(120), 1);
            assertEquals(1, callback.size());
            callback.getFirst().claimCallback(BASE.plusSeconds(1), "callback");
            callback.getFirst().markCallbackFailed("timeout", BASE.plusSeconds(2),
                    BASE.plusSeconds(10));
            repository.flush();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            assertTrue(repository.lockCallbackClaimable(BASE.plusSeconds(3),
                    BASE.minusSeconds(120), 1).isEmpty());
            var due = repository.lockCallbackClaimable(BASE.plusSeconds(11),
                    BASE.minusSeconds(120), 1);
            assertEquals(1, due.size());
            due.getFirst().claimCallback(BASE.plusSeconds(11), "callback");
            repository.flush();
        });
        var persisted = repository.findById(entity.eventId()).orElseThrow();
        assertEquals(2, persisted.callbackAttemptCount());
        assertEquals(PaymentOutboxEntity.Status.PROCESSING, persisted.callbackStatus());
        assertEquals(PaymentOutboxEntity.Status.PENDING, persisted.status());
    }

    @Test
    void genericAndCallbackClaimCanRunConcurrentlyForSameEvent() throws Exception {
        PaymentOutboxEntity entity = entity(event("12345678"), aggregate("abcdabcd"), BASE);
        repository.saveAndFlush(entity);
        jdbc.update("UPDATE payment_outbox_events SET event_type = 'payment.domain-event' WHERE event_id = ?",
                entity.eventId());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var generic = executor.submit(() -> {
                start.await();
                for (int n = 0; n < 30; n++) {
                    var claimed = claimService.claimAvailable(BASE.plusSeconds(1),
                            BASE.minusSeconds(120), 1, "generic");
                    if (!claimed.isEmpty()) return claimed;
                    Thread.sleep(20);
                }
                return List.<PaymentOutboxClaim>of();
            });
            var callback = executor.submit(() -> {
                start.await();
                for (int n = 0; n < 30; n++) {
                    var ids = new TransactionTemplate(transactionManager).execute(tx -> {
                        var claimed = repository.lockCallbackClaimable(BASE.plusSeconds(1),
                                BASE.minusSeconds(120), 1);
                        for (var row : claimed) row.claimCallback(BASE.plusSeconds(1), "callback");
                        repository.flush();
                        return claimed.stream().map(PaymentOutboxEntity::eventId).toList();
                    });
                    if (ids != null && !ids.isEmpty()) return ids;
                    Thread.sleep(20);
                }
                return List.<UUID>of();
            });
            start.countDown();
            assertEquals(1, generic.get().size());
            assertEquals(1, callback.get().size());
        }
        var persisted = repository.findById(entity.eventId()).orElseThrow();
        assertEquals(PaymentOutboxEntity.Status.PROCESSING, persisted.status());
        assertEquals(PaymentOutboxEntity.Status.PROCESSING, persisted.callbackStatus());
    }

    @Test
    void callbackClaimRespectsPerPaymentOrdering() {
        UUID payment = aggregate("aaaaaaaa");
        PaymentOutboxEntity first = entity(event("88888888"), payment, BASE);
        PaymentOutboxEntity second = entity(event("99999999"), payment, BASE.plusSeconds(1));
        repository.saveAllAndFlush(List.of(first, second));
        jdbc.update("UPDATE payment_outbox_events SET event_type = 'payment.domain-event' "
                + "WHERE event_id IN (?, ?)", first.eventId(), second.eventId());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var claimed = repository.lockCallbackClaimable(BASE.plusSeconds(3),
                    BASE.minusSeconds(120), 10);
            assertEquals(1, claimed.size());
            assertEquals(first.eventId(), claimed.getFirst().eventId());
            claimed.getFirst().claimCallback(BASE.plusSeconds(3), "callback");
            claimed.getFirst().markCallbackPublished(BASE.plusSeconds(4));
            repository.flush();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var next = repository.lockCallbackClaimable(BASE.plusSeconds(5),
                    BASE.minusSeconds(120), 10);
            assertEquals(1, next.size());
            assertEquals(second.eventId(), next.getFirst().eventId());
        });
    }

    @Test
    void staleCallbackAcknowledgementsAreRejectedAfterLeaseRecovery() {
        PaymentOutboxEntity row = entity(event("a1a1a1a1"), aggregate("b1b1b1b1"), BASE);
        repository.saveAndFlush(row);
        jdbc.update("UPDATE payment_outbox_events SET event_type='payment.domain-event' WHERE event_id=?", row.eventId());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var rows = repository.lockCallbackClaimable(BASE.plusSeconds(1), BASE.minusSeconds(120), 1);
            assertEquals(1, rows.size());
            rows.getFirst().claimCallback(BASE.plusSeconds(1), "same-worker");
            repository.flush();
        });
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var rows = repository.lockCallbackClaimable(BASE.plusSeconds(40), BASE.plusSeconds(10), 1);
            assertEquals(1, rows.size());
            rows.getFirst().claimCallback(BASE.plusSeconds(40), "same-worker");
            repository.flush();
        });
        var settings = new com.sixpay.payment.infrastructure.callback.PaymentCallbackProperties();
        settings.setWorkerId("same-worker");
        var coordinator = new com.sixpay.payment.infrastructure.callback.relay.PaymentCallbackOutboxCoordinator(
                repository, settings, () -> BASE.plusSeconds(41));
        var transactions = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(tx ->
                coordinator.markPublished(row.eventId(), 1)));
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(tx ->
                coordinator.markFailed(row.eventId(), 1,
                        new IllegalStateException("stale failure"))));
        assertEquals(PaymentOutboxEntity.Status.PROCESSING,
                repository.findById(row.eventId()).orElseThrow().callbackStatus());
        transactions.executeWithoutResult(tx -> coordinator.markPublished(row.eventId(), 2));
        var stored = repository.findById(row.eventId()).orElseThrow();
        assertEquals(PaymentOutboxEntity.Status.PUBLISHED, stored.callbackStatus());
        assertEquals(PaymentOutboxEntity.Status.PENDING, stored.status());
        assertEquals(2, stored.callbackAttemptCount());
    }

    @Test
    void callbackTimeoutRetryAndGenericPublicationCompleteIndependently() {
        PaymentOutboxEntity row = entity(event("a2a2a2a2"), aggregate("b2b2b2b2"), BASE);
        repository.saveAndFlush(row);
        jdbc.update("UPDATE payment_outbox_events SET event_type='payment.domain-event' WHERE event_id=?", row.eventId());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var rows = repository.lockCallbackClaimable(BASE.plusSeconds(1), BASE.minusSeconds(120), 1);
            assertEquals(1, rows.size());
            rows.getFirst().claimCallback(BASE.plusSeconds(1), "callback");
            repository.flush();
        });
        var settings = new com.sixpay.payment.infrastructure.callback.PaymentCallbackProperties();
        settings.setWorkerId("callback");
        var now = new java.util.concurrent.atomic.AtomicReference<>(BASE.plusSeconds(2));
        var callbacks = new com.sixpay.payment.infrastructure.callback.relay.PaymentCallbackOutboxCoordinator(
                repository, settings, now::get);
        var transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(tx -> callbacks.markFailed(
                row.eventId(), 1, new java.net.SocketTimeoutException("response lost")));
        var generic = claimService.claimAvailable(BASE.plusSeconds(3), BASE.minusSeconds(120), 1, "generic");
        assertEquals(1, generic.size());
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            var locked = repository.findById(row.eventId()).orElseThrow();
            locked.markPublished(BASE.plusSeconds(3));
            repository.flush();
        });
        assertEquals(PaymentOutboxEntity.Status.FAILED,
                repository.findById(row.eventId()).orElseThrow().callbackStatus());
        now.set(BASE.plusSeconds(20));
        int recoveredCount = transactions.execute(
                tx -> callbacks.claim().size()
        );

        assertEquals(1, recoveredCount);

        assertEquals(
                2,
                repository.findById(row.eventId())
                        .orElseThrow()
                        .callbackAttemptCount()
        );
        transactions.executeWithoutResult(tx -> callbacks.markPublished(row.eventId(), 2));
        var stored = repository.findById(row.eventId()).orElseThrow();
        assertEquals(PaymentOutboxEntity.Status.PUBLISHED, stored.status());
        assertEquals(PaymentOutboxEntity.Status.PUBLISHED, stored.callbackStatus());
        assertEquals(2, stored.callbackAttemptCount());
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
        PaymentCustomerSubscriptionPort paymentCustomerSubscriptionPort() {
            return mock(PaymentCustomerSubscriptionPort.class);
        }

        @Bean
        CurrentUserProvider currentUserProvider() {
            return Optional::<AuthenticatedUser>empty;
        }
    }
}
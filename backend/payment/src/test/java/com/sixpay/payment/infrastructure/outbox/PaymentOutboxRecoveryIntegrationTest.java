package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.payment.infrastructure.outbox.claim
        .PaymentOutboxClaimService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes =
                PaymentOutboxRecoveryIntegrationTest
                        .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PaymentOutboxRecoveryIntegrationTest {

    private static final String PROJECTION_TYPE =
            "payment.observation-projection";

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
    void crashAfterClaimIsRecoveredByAnotherWorker() {
        PaymentOutboxEntity row = entity(
                event("11111111"),
                aggregate("aaaaaaaa"),
                PROJECTION_TYPE,
                BASE
        );

        repository.saveAndFlush(row);

        var firstClaim =
                claimService.claimAvailableByEventType(
                        PROJECTION_TYPE,
                        BASE.plusSeconds(1),
                        BASE.minusSeconds(120),
                        1,
                        "worker-before-crash"
                );

        assertEquals(1, firstClaim.size());
        assertEquals(
                "worker-before-crash",
                firstClaim.getFirst().claimedBy()
        );

        /*
         * No completion call simulates a process crash after claim.
         * A fresh worker later considers the abandoned claim stale.
         */
        var recovered =
                claimService.claimAvailableByEventType(
                        PROJECTION_TYPE,
                        BASE.plusSeconds(301),
                        BASE.plusSeconds(2),
                        1,
                        "worker-after-restart"
                );

        assertEquals(1, recovered.size());
        assertEquals(
                row.eventId(),
                recovered.getFirst().eventId()
        );
        assertEquals(
                2,
                recovered.getFirst().attempt()
        );
        assertEquals(
                "worker-after-restart",
                recovered.getFirst().claimedBy()
        );
    }

    @Test
    void publishedRowIsNotClaimedAgainOnReplay() {
        PaymentOutboxEntity row = entity(
                event("11111111"),
                aggregate("aaaaaaaa"),
                PROJECTION_TYPE,
                BASE
        );

        repository.saveAndFlush(row);

        claimService.claimAvailableByEventType(
                PROJECTION_TYPE,
                BASE.plusSeconds(1),
                BASE.minusSeconds(120),
                1,
                "worker-a"
        );

        new TransactionTemplate(
                transactionManager
        ).executeWithoutResult(status -> {
            PaymentOutboxEntity claimed =
                    repository.findById(
                            row.eventId()
                    ).orElseThrow();

            claimed.markPublished(
                    BASE.plusSeconds(2)
            );

            repository.flush();
        });

        assertTrue(
                claimService.claimAvailableByEventType(
                        PROJECTION_TYPE,
                        BASE.plusSeconds(3),
                        BASE.minusSeconds(120),
                        10,
                        "worker-b"
                ).isEmpty()
        );
    }

    @Test
    void customerProjectionClaimIgnoresOtherOutboxContracts() {
        PaymentOutboxEntity projection = entity(
                event("11111111"),
                aggregate("aaaaaaaa"),
                PROJECTION_TYPE,
                BASE
        );

        PaymentOutboxEntity other = entity(
                event("22222222"),
                aggregate("bbbbbbbb"),
                "payment.domain-event",
                BASE
        );

        repository.saveAllAndFlush(
                List.of(projection, other)
        );

        var claims =
                claimService.claimAvailableByEventType(
                        PROJECTION_TYPE,
                        BASE.plusSeconds(1),
                        BASE.minusSeconds(120),
                        10,
                        "worker-a"
                );

        assertEquals(1, claims.size());
        assertEquals(
                projection.eventId(),
                claims.getFirst().eventId()
        );
        assertEquals(
                PROJECTION_TYPE,
                claims.getFirst().eventType()
        );
    }

    private PaymentOutboxEntity entity(
            UUID eventId,
            UUID paymentId,
            String eventType,
            Instant occurredAt
    ) {
        ensurePaymentExists(paymentId);

        return PaymentOutboxEntity.create(
                eventId,
                paymentId,
                eventType,
                1,
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                "{\"safe\":true}",
                occurredAt,
                occurredAt
        );
    }

    private void ensurePaymentExists(UUID paymentId) {
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

    private static String paymentReference(UUID paymentId) {
        return "PAY-"
                + paymentId.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();
    }

    private static UUID event(String prefix) {
        return UUID.fromString(
                prefix + "-1111-4111-8111-111111111111"
        );
    }

    private static UUID aggregate(String prefix) {
        return UUID.fromString(
                prefix + "-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
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

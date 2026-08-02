package com.sixpay.payment;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.infrastructure.outbox.PaymentDomainEventMapper;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = PaymentOutboxAtomicityIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentOutboxAtomicityIT {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-01T16:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private PaymentDomainEventMapper eventMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void commitsPaymentAndOutboxAtomically() {
        UUID paymentId = UUID.randomUUID();
        String reference = reference(paymentId);
        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(status -> {
            insertPayment(paymentId, reference);
            outboxRepository.save(
                    eventMapper.toOutboxEntity(
                            event(paymentId, reference),
                            OCCURRED_AT.plusSeconds(1)
                    )
            );
        });

        assertThat(count("payments", "payment_id", paymentId)).isOne();
        assertThat(
                count("payment_outbox_events", "aggregate_id", paymentId)
        ).isOne();
    }

    @Test
    void rollsBackPaymentAndOutboxAtomically() {
        UUID paymentId = UUID.randomUUID();
        String reference = reference(paymentId);
        TransactionTemplate tx =
                new TransactionTemplate(transactionManager);

        assertThatThrownBy(() ->
                tx.executeWithoutResult(status -> {
                    insertPayment(paymentId, reference);
                    outboxRepository.save(
                            eventMapper.toOutboxEntity(
                                    event(paymentId, reference),
                                    OCCURRED_AT.plusSeconds(1)
                            )
                    );
                    throw new IntentionalRollback();
                })
        ).isInstanceOf(IntentionalRollback.class);

        assertThat(count("payments", "payment_id", paymentId)).isZero();
        assertThat(
                count("payment_outbox_events", "aggregate_id", paymentId)
        ).isZero();
    }

    private void insertPayment(UUID id, String reference) {
        jdbc.update("""
                INSERT INTO payments (
                    payment_id, public_payment_reference, payment_source,
                    external_payment_reference, external_subscription_reference,
                    financial_institution_code, requested_amount,
                    requested_currency, status, business_version,
                    received_at, updated_at, finalized_at,
                    state_payload, persistence_version
                ) VALUES (
                    ?, ?, 'TRESOR_PAY', ?, ?, 'SIXPAY_BANK',
                    1000.00, 'XAF', 'RECEIVED', 1,
                    TIMESTAMPTZ '2026-08-01 16:00:00+00',
                    TIMESTAMPTZ '2026-08-01 16:00:00+00',
                    NULL, '{"schemaVersion":1}'::jsonb, 0
                )
                """,
                id,
                reference,
                "EXT-" + id,
                "SUB-" + id
        );
    }

    private PaymentDomainEvent event(UUID id, String reference) {
        return new TestPaymentEvent(
                new PaymentEventMetadata(
                        UUID.randomUUID(),
                        new PaymentId(id),
                        PublicPaymentReference.of(reference),
                        CorrelationId.of("correlation-" + id),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        OCCURRED_AT
                )
        );
    }

    private int count(String table, String column, UUID id) {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id
        );
        return value == null ? 0 : value;
    }

    private static String reference(UUID id) {
        return "PAY-" + id.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(PaymentModuleConfiguration.class)
    static class TestApplication {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () -> Optional.<AuthenticatedUser>empty();
        }
    }

    private record TestPaymentEvent(
            PaymentEventMetadata metadata
    ) implements PaymentDomainEvent {
    }

    private static final class IntentionalRollback
            extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

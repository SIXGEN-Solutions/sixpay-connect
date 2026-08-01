package com.sixpay.payment.infrastructure.audit;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = PaymentAuditAtomicityIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentAuditAtomicityIT {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-01T12:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
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
    private PaymentAuditAdapter auditAdapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void persistsPaymentAndAuditInTheSameTransaction() {
        UUID paymentId = UUID.randomUUID();
        String paymentReference =
                publicPaymentReference(paymentId);

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            insertPayment(
                    paymentId,
                    paymentReference
            );

            auditAdapter.append(
                    paymentEvent(
                            paymentId,
                            paymentReference
                    )
            );
        });

        assertThat(
                countPayments(paymentId)
        ).isOne();

        assertThat(
                countAuditEntries(paymentId)
        ).isOne();
    }

    @Test
    void rollsBackPaymentAndAuditAtomically() {
        UUID paymentId = UUID.randomUUID();
        String paymentReference =
                publicPaymentReference(paymentId);

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        assertThatThrownBy(() ->
                transaction.executeWithoutResult(status -> {
                    insertPayment(
                            paymentId,
                            paymentReference
                    );

                    auditAdapter.append(
                            paymentEvent(
                                    paymentId,
                                    paymentReference
                            )
                    );

                    throw new IntentionalRollback();
                })
        ).isInstanceOf(IntentionalRollback.class);

        assertThat(
                countPayments(paymentId)
        ).isZero();

        assertThat(
                countAuditEntries(paymentId)
        ).isZero();
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
                    TIMESTAMPTZ '2026-08-01 12:00:00+00',
                    TIMESTAMPTZ '2026-08-01 12:00:00+00',
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

    private PaymentDomainEvent paymentEvent(
            UUID paymentId,
            String paymentReference
    ) {
        PaymentEventMetadata metadata =
                new PaymentEventMetadata(
                        UUID.randomUUID(),
                        new PaymentId(paymentId),
                        PublicPaymentReference.of(
                                paymentReference
                        ),
                        CorrelationId.of(
                                "correlation-" + paymentId
                        ),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        OCCURRED_AT
                );

        return new TestPaymentEvent(metadata);
    }

    private int countPayments(UUID paymentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM payments
                WHERE payment_id = ?
                """,
                Integer.class,
                paymentId
        );

        return count == null ? 0 : count;
    }

    private int countAuditEntries(UUID paymentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM payment_audit
                WHERE payment_id = ?
                """,
                Integer.class,
                paymentId
        );

        return count == null ? 0 : count;
    }

    private static String publicPaymentReference(
            UUID paymentId
    ) {
        String identifier = paymentId
                .toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();

        return "PAY-" + identifier;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(
            PaymentModuleConfiguration.class
    )
    static class TestApplication {
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
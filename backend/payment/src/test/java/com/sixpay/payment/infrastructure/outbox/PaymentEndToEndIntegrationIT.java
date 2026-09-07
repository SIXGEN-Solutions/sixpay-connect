package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentEndToEndIntegrationIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentEndToEndIntegrationIT {

    private static final Instant STARTED_AT =
            Instant.parse("2026-08-01T20:00:00Z");

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
    private JdbcTemplate jdbc;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private PaymentIntegrationMapper integrationMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AccountingProbe accountingProbe;

    @Autowired
    private NotificationProbe notificationProbe;

    @Test
    void durablePaymentOutboxPreservesAtomicT0AndDownstreamOrdering() {
        UUID paymentId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        String paymentReference = reference(paymentId);

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            insertPayment(paymentId, paymentReference);

            saveEvent(
                    paymentId,
                    correlationId,
                    "PaymentReceived",
                    "{\"source\":\"TRESOR_PAY\",\"paymentReference\":\""
                            + paymentReference + "\"}",
                    STARTED_AT
            );

            saveEvent(
                    paymentId,
                    correlationId,
                    "PaymentEventOutcomeRecorded",
                    "{\"outcome\":\"COMPLETED\","
                            + "\"bankPostingReference\":\"BANK-POST-001\"}",
                    STARTED_AT.plusSeconds(1)
            );

            saveEvent(
                    paymentId,
                    correlationId,
                    "PaymentEndOfDayTrackingRequested",
                    "{\"bankPostingReference\":\"BANK-POST-001\"}",
                    STARTED_AT.plusSeconds(2)
            );

            saveEvent(
                    paymentId,
                    correlationId,
                    "PaymentFinalResultAvailable",
                    "{\"resultType\":\"POSTED_PENDING_TFJ\","
                            + "\"paymentReference\":\""
                            + paymentReference + "\"}",
                    STARTED_AT.plusSeconds(3)
            );
        });

        List<IntegrationEventEnvelope> envelopes =
                outboxRepository.findAll()
                        .stream()
                        .filter(entity ->
                                entity.aggregateId().equals(paymentId)
                        )
                        .sorted(
                                Comparator.comparing(
                                        PaymentOutboxEntity::occurredAt
                                )
                        )
                        .map(integrationMapper::toEnvelope)
                        .toList();

        assertThat(
                envelopes.stream()
                        .map(IntegrationEventEnvelope::eventType)
        ).containsExactly(
                "PaymentReceived",
                "PaymentEventOutcomeRecorded",
                "PaymentEndOfDayTrackingRequested",
                "PaymentFinalResultAvailable"
        );

        envelopes.forEach(envelope -> {
            if ("PaymentEndOfDayTrackingRequested".equals(
                    envelope.eventType()
            )) {
                accountingProbe.accept(envelope);
            }

            if ("PaymentFinalResultAvailable".equals(
                    envelope.eventType()
            )) {
                notificationProbe.accept(envelope);
            }
        });

        assertThat(accountingProbe.received()).hasSize(1);
        assertThat(notificationProbe.received()).hasSize(1);

        assertThat(
                countRows("payments", "payment_id", paymentId)
        ).isOne();

        assertThat(
                countRows(
                        "payment_outbox_events",
                        "aggregate_id",
                        paymentId
                )
        ).isEqualTo(4);
    }

    private void saveEvent(
            UUID paymentId,
            String correlationId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        outboxRepository.save(
                PaymentOutboxEntity.create(
                        UUID.randomUUID(),
                        paymentId,
                        eventType,
                        1,
                        correlationId,
                        payload,
                        occurredAt,
                        occurredAt
                )
        );
    }

    private void insertPayment(
            UUID paymentId,
            String paymentReference
    ) {
        jdbc.update(
                "INSERT INTO payments ("
                        + "payment_id,"
                        + "public_payment_reference,"
                        + "payment_source,"
                        + "external_payment_reference,"
                        + "external_subscription_reference,"
                        + "financial_institution_code,"
                        + "requested_amount,"
                        + "requested_currency,"
                        + "status,"
                        + "business_version,"
                        + "received_at,"
                        + "updated_at,"
                        + "finalized_at,"
                        + "state_payload,"
                        + "persistence_version"
                        + ") VALUES ("
                        + "?, ?, 'TRESOR_PAY', ?, ?, 'SIXPAY_BANK', "
                        + "1000.00, 'XAF', 'RECEIVED', 1, "
                        + "TIMESTAMPTZ '2026-08-01 20:00:00+00', "
                        + "TIMESTAMPTZ '2026-08-01 20:00:00+00', "
                        + "NULL, '{\"schemaVersion\":1}'::jsonb, 0)",
                paymentId,
                paymentReference,
                "EXT-" + paymentId,
                "SUB-" + paymentId
        );
    }

    private int countRows(
            String table,
            String column,
            UUID id
    ) {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM "
                        + table
                        + " WHERE "
                        + column
                        + " = ?",
                Integer.class,
                id
        );

        return value == null ? 0 : value;
    }

    private static String reference(UUID paymentId) {
        return "PAY-"
                + paymentId.toString()
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
            return Optional::<AuthenticatedUser>empty;
        }

        @Bean
        AccountingProbe accountingProbe() {
            return new AccountingProbe();
        }

        @Bean
        NotificationProbe notificationProbe() {
            return new NotificationProbe();
        }
    }

    static final class AccountingProbe {

        private final List<IntegrationEventEnvelope> received =
                new CopyOnWriteArrayList<>();

        void accept(IntegrationEventEnvelope envelope) {
            received.add(envelope);
        }

        List<IntegrationEventEnvelope> received() {
            return List.copyOf(received);
        }
    }

    static final class NotificationProbe {

        private final List<IntegrationEventEnvelope> received =
                new CopyOnWriteArrayList<>();

        void accept(IntegrationEventEnvelope envelope) {
            received.add(envelope);
        }

        List<IntegrationEventEnvelope> received() {
            return List.copyOf(received);
        }
    }
}

package com.sixpay.tests.paymentcustomer;

import com.sixpay.bootstrap.integration.customer.ObservedCustomerProjectionIntegrationConfiguration;
import com.sixpay.bootstrap.integration.customer.PaymentObservedCustomerOutboxConsumer;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerRepository;
import com.sixpay.customer.observation.configuration.ObservedCustomerPersistenceConfiguration;
import com.sixpay.customer.observation.configuration.ObservedCustomerProjectionResilienceConfiguration;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedCustomerSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedPaymentSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ProcessedObservationEventSpringDataRepository;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection.ProjectionPaymentStatus;
import com.sixpay.payment.application.port.output.query.PaymentObservedCustomerLinkPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentCustomerObservationIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("payment-customer-test")
@Testcontainers
class PaymentCustomerObservationIT {

    private static final Instant T0 =
            Instant.parse("2026-08-16T14:00:00Z");

    private static final UUID CORRELATION_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final String NIU =
            "NIU-00000001";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            )
                    .withDatabaseName("sixpay_payment_customer")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test")
                    .withInitScript(
                            "payment-customer-schema.sql"
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
    private PaymentObservedCustomerOutboxConsumer consumer;

    @Autowired
    private ObservedCustomerRepository customerRepository;

    @Autowired
    private ObservedCustomerSpringDataRepository customerJpaRepository;

    @Autowired
    private ObservedPaymentSpringDataRepository paymentJpaRepository;

    @Autowired
    private ProcessedObservationEventSpringDataRepository eventJpaRepository;

    @Autowired
    private RecordingPaymentObservedCustomerLinkPort linkPort;

    @BeforeEach
    void reset() {
        eventJpaRepository.deleteAllInBatch();
        paymentJpaRepository.deleteAllInBatch();
        customerJpaRepository.deleteAllInBatch();
        linkPort.clear();
    }

    @Test
    void debitConfirmedPaymentIsProjectedIntoObservedCustomer() {
        UUID eventId =
                UUID.fromString(
                        "21111111-1111-4111-8111-111111111111"
                );

        UUID paymentId =
                UUID.fromString(
                        "31111111-1111-4111-8111-111111111111"
                );

        consumer.accept(
                event(
                        eventId,
                        paymentId,
                        "PAY-PC-001",
                        ProjectionPaymentStatus.DEBITED,
                        null,
                        ObservedCustomerProjectionEventType
                                .PAYMENT_DEBIT_CONFIRMED,
                        6L,
                        T0.plusSeconds(30)
                )
        );

        var observedCustomer =
                customerRepository
                        .findByNormalizedNiu(NIU)
                        .orElseThrow();

        assertThat(observedCustomer.totalPayments())
                .isEqualTo(1);

        assertThat(observedCustomer.payments())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.paymentId())
                            .isEqualTo(paymentId);
                    assertThat(payment.paymentReference())
                            .isEqualTo("PAY-PC-001");
                    assertThat(payment.status())
                            .isEqualTo(
                                    ObservedPaymentStatus.DEBITED
                            );
                    assertThat(payment.failureReasonCode())
                            .isNull();
                    assertThat(payment.currency())
                            .isEqualTo("XAF");
                    assertThat(payment.amount())
                            .isEqualByComparingTo(
                                    "12500.00"
                            );
                });

        assertThat(eventJpaRepository.count())
                .isEqualTo(1);

        assertThat(linkPort.links())
                .singleElement()
                .satisfies(link -> {
                    assertThat(link.paymentId())
                            .isEqualTo(paymentId);
                    assertThat(link.observedCustomerId())
                            .isEqualTo(
                                    observedCustomer.id().value()
                            );
                });
    }

    @Test
    void failedPaymentPreservesFailureSemanticsAcrossBoundary() {
        UUID eventId =
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                );

        UUID paymentId =
                UUID.fromString(
                        "32222222-2222-4222-8222-222222222222"
                );

        consumer.accept(
                event(
                        eventId,
                        paymentId,
                        "PAY-PC-002",
                        ProjectionPaymentStatus.FAILED,
                        "BANK_REJECTED",
                        ObservedCustomerProjectionEventType
                                .PAYMENT_FAILED,
                        7L,
                        T0.plusSeconds(45)
                )
        );

        ObservedPaymentReference observedPayment =
                customerRepository
                        .findByNormalizedNiu(NIU)
                        .orElseThrow()
                        .payments()
                        .getFirst();

        assertThat(observedPayment.paymentId())
                .isEqualTo(paymentId);
        assertThat(observedPayment.status())
                .isEqualTo(
                        ObservedPaymentStatus.FAILED
                );
        assertThat(observedPayment.failureReasonCode())
                .isEqualTo("BANK_REJECTED");
    }

    @Test
    void replayOfSamePaymentProjectionIsIdempotent() {
        UUID eventId =
                UUID.fromString(
                        "23333333-3333-4333-8333-333333333333"
                );

        UUID paymentId =
                UUID.fromString(
                        "33333333-3333-4333-8333-333333333333"
                );

        ObservedCustomerProjectionEvent event =
                event(
                        eventId,
                        paymentId,
                        "PAY-PC-003",
                        ProjectionPaymentStatus.DEBITED,
                        null,
                        ObservedCustomerProjectionEventType
                                .PAYMENT_DEBIT_CONFIRMED,
                        6L,
                        T0.plusSeconds(60)
                );

        consumer.accept(event);
        consumer.accept(event);

        var observedCustomer =
                customerRepository
                        .findByNormalizedNiu(NIU)
                        .orElseThrow();

        assertThat(observedCustomer.totalPayments())
                .isEqualTo(1);
        assertThat(observedCustomer.payments())
                .hasSize(1);
        assertThat(paymentJpaRepository.count())
                .isEqualTo(1);
        assertThat(eventJpaRepository.count())
                .isEqualTo(1);
    }

    private static ObservedCustomerProjectionEvent event(
            UUID eventId,
            UUID paymentId,
            String paymentReference,
            ProjectionPaymentStatus paymentStatus,
            String failureReasonCode,
            ObservedCustomerProjectionEventType eventType,
            long aggregateVersion,
            Instant occurredAt
    ) {
        ObservedCustomerProjectionPayload payload =
                new ObservedCustomerProjectionPayload(
                        paymentReference,
                        NIU,
                        "ALICE CUSTOMER",
                        "***1234",
                        "a***@example.com",
                        "LAREGIONALE",
                        "v1:"
                                + "a".repeat(64),
                        "****1234",
                        new BigDecimal("12500.00"),
                        "XAF",
                        paymentStatus,
                        failureReasonCode,
                        T0,
                        occurredAt
                );

        return ObservedCustomerProjectionEvent.versionOne(
                eventId,
                paymentId,
                aggregateVersion,
                eventType,
                payload,
                CORRELATION_ID.toString(),
                occurredAt
        );
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import({
            ObservedCustomerProjectionResilienceConfiguration.class,
            ObservedCustomerPersistenceConfiguration.class,
            ObservedCustomerProjectionIntegrationConfiguration.class,
            TestSupportConfiguration.class
    })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupportConfiguration {

        @Bean
        RecordingPaymentObservedCustomerLinkPort
        recordingPaymentObservedCustomerLinkPort() {
            return new RecordingPaymentObservedCustomerLinkPort();
        }

        @Bean
        PaymentObservedCustomerLinkPort
        paymentObservedCustomerLinkPort(
                RecordingPaymentObservedCustomerLinkPort recorder
        ) {
            return recorder;
        }
    }

    static final class RecordingPaymentObservedCustomerLinkPort
            implements PaymentObservedCustomerLinkPort {

        private final CopyOnWriteArrayList<Link>
                links = new CopyOnWriteArrayList<>();

        @Override
        public void link(
                UUID paymentId,
                UUID observedCustomerId
        ) {
            links.add(
                    new Link(
                            paymentId,
                            observedCustomerId
                    )
            );
        }

        void clear() {
            links.clear();
        }

        List<Link> links() {
            return List.copyOf(links);
        }

        record Link(
                UUID paymentId,
                UUID observedCustomerId
        ) {
        }
    }
}

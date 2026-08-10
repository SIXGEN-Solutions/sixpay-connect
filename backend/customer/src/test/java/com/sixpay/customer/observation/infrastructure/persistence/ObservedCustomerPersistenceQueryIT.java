package com.sixpay.customer.observation.infrastructure.persistence;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerRepository;
import com.sixpay.customer.observation.application.port.output.ObservedPaymentRepository;
import com.sixpay.customer.observation.application.port.output.query.ObservedCustomerPaymentQueryRepository;
import com.sixpay.customer.observation.application.port.output.query.ObservedCustomerQueryRepository;
import com.sixpay.customer.observation.application.query.ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query.ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query.ObservedCustomerSort;
import com.sixpay.customer.observation.application.service.ObservedCustomerProjectionService;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.customer.observation.infrastructure.persistence.adapter.JpaObservedCustomerRepositoryAdapter;
import com.sixpay.customer.observation.infrastructure.persistence.adapter.JpaObservedPaymentRepositoryAdapter;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedAccountJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerInstitutionJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedPaymentJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ProcessedObservationEventJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.mapper.ObservedCustomerPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.persistence.protection.AesGcmObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.persistence.protection.ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedCustomerSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedPaymentSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ProcessedObservationEventSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.query.adapter.JpaObservedCustomerPaymentQueryAdapter;
import com.sixpay.customer.observation.infrastructure.query.adapter.JpaObservedCustomerQueryAdapter;
import com.sixpay.customer.observation.infrastructure.query.mapper.ObservedCustomerQueryRowMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ObservedCustomerPersistenceQueryIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class ObservedCustomerPersistenceQueryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            );

    private static final UUID CUSTOMER_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID FIRST_EVENT_ID =
            UUID.fromString(
                    "21111111-1111-4111-8111-111111111111"
            );

    private static final UUID FIRST_PAYMENT_ID =
            UUID.fromString(
                    "31111111-1111-4111-8111-111111111111"
            );

    private static final UUID SECOND_EVENT_ID =
            UUID.fromString(
                    "22222222-2222-4222-8222-222222222222"
            );

    private static final UUID SECOND_PAYMENT_ID =
            UUID.fromString(
                    "32222222-2222-4222-8222-222222222222"
            );

    private static final UUID CORRELATION_ID =
            UUID.fromString(
                    "41111111-1111-4111-8111-111111111111"
            );

    private static final Instant T0 =
            Instant.parse("2026-08-09T18:00:00Z");

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
    private ObservedCustomerRepository customerRepository;

    @Autowired
    private ObservedPaymentRepository paymentRepository;

    @Autowired
    private ObservedCustomerQueryRepository customerQueryRepository;

    @Autowired
    private ObservedCustomerPaymentQueryRepository paymentQueryRepository;

    @Autowired
    private ObservedCustomerSpringDataRepository customerJpaRepository;

    @Autowired
    private ObservedPaymentSpringDataRepository paymentJpaRepository;

    @Autowired
    private ProcessedObservationEventSpringDataRepository eventJpaRepository;

    private ObservedCustomerProjectionService projectionService;

    @BeforeEach
    void cleanDatabase() {
        eventJpaRepository.deleteAllInBatch();
        paymentJpaRepository.deleteAllInBatch();
        customerJpaRepository.deleteAllInBatch();

        projectionService =
                new ObservedCustomerProjectionService(
                        customerRepository,
                        paymentRepository,
                        () -> ObservedCustomerId.of(CUSTOMER_ID)
                );
    }

    @Test
    void persistsReloadsAndFindsCustomerByProtectedNiu() {
        projectionService.observe(
                command(
                        FIRST_EVENT_ID,
                        FIRST_PAYMENT_ID,
                        "PAY-CUST-001",
                        ObservedPaymentStatus.DEBITED,
                        T0
                )
        );

        assertThat(
                customerRepository.findByNormalizedNiu(
                        "NIU-00000001"
                )
        )
                .isPresent()
                .get()
                .satisfies(customer -> {
                    assertThat(customer.id())
                            .isEqualTo(
                                    ObservedCustomerId.of(
                                            CUSTOMER_ID
                                    )
                            );
                    assertThat(customer.payments())
                            .hasSize(1);
                    assertThat(
                            customer.totalPayments()
                    ).isEqualTo(1);
                });

        assertThat(customerJpaRepository.count())
                .isEqualTo(1);
        assertThat(paymentJpaRepository.count())
                .isEqualTo(1);
        assertThat(eventJpaRepository.count())
                .isEqualTo(1);
    }

    @Test
    void customerSearchUsesStableKeysetPagination() {
        projectionService.observe(
                command(
                        FIRST_EVENT_ID,
                        FIRST_PAYMENT_ID,
                        "PAY-CUST-001",
                        ObservedPaymentStatus.DEBITED,
                        T0
                )
        );

        var firstPage =
                customerQueryRepository.search(
                        new ObservedCustomerSearchCriteria(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                ObservedCustomerSort
                                        .FIRST_OBSERVED_AT_ASC,
                                1,
                                T0.plusSeconds(120),
                                null
                        )
                );

        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.items().getFirst()
                .observedCustomerId())
                .isEqualTo(
                        ObservedCustomerId.of(
                                CUSTOMER_ID
                        )
                );
        assertThat(firstPage.hasMore()).isFalse();
        assertThat(firstPage.nextPosition()).isNull();
    }

    @Test
    void paymentQueryOrdersNewestFirstAndSupportsCursorBoundary() {
        projectionService.observe(
                command(
                        FIRST_EVENT_ID,
                        FIRST_PAYMENT_ID,
                        "PAY-CUST-001",
                        ObservedPaymentStatus.DEBITED,
                        T0
                )
        );

        projectionService.observe(
                command(
                        SECOND_EVENT_ID,
                        SECOND_PAYMENT_ID,
                        "PAY-CUST-002",
                        ObservedPaymentStatus.FAILED,
                        T0.plusSeconds(30)
                )
        );

        ObservedCustomerId customerId =
                ObservedCustomerId.of(CUSTOMER_ID);

        var firstPage =
                paymentQueryRepository.findByCustomerId(
                        new ObservedCustomerPaymentCriteria(
                                customerId,
                                null,
                                null,
                                null,
                                1,
                                T0.plusSeconds(120),
                                null
                        )
                );

        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.hasMore()).isTrue();
        assertThat(firstPage.nextPosition()).isNotNull();
        assertThat(firstPage.items().getFirst()
                .paymentId())
                .isEqualTo(SECOND_PAYMENT_ID);

        var secondPage =
                paymentQueryRepository.findByCustomerId(
                        new ObservedCustomerPaymentCriteria(
                                customerId,
                                null,
                                null,
                                null,
                                1,
                                T0.plusSeconds(120),
                                firstPage.nextPosition()
                        )
                );

        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().getFirst()
                .paymentId())
                .isEqualTo(FIRST_PAYMENT_ID);
        assertThat(secondPage.hasMore()).isFalse();
    }

    @Test
    void paymentQueryFiltersByStatusAndDateRange() {
        projectionService.observe(
                command(
                        FIRST_EVENT_ID,
                        FIRST_PAYMENT_ID,
                        "PAY-CUST-001",
                        ObservedPaymentStatus.DEBITED,
                        T0
                )
        );

        projectionService.observe(
                command(
                        SECOND_EVENT_ID,
                        SECOND_PAYMENT_ID,
                        "PAY-CUST-002",
                        ObservedPaymentStatus.FAILED,
                        T0.plusSeconds(30)
                )
        );

        var slice =
                paymentQueryRepository.findByCustomerId(
                        new ObservedCustomerPaymentCriteria(
                                ObservedCustomerId.of(
                                        CUSTOMER_ID
                                ),
                                ObservedPaymentStatus.FAILED,
                                T0.plusSeconds(20),
                                T0.plusSeconds(40),
                                10,
                                T0.plusSeconds(120),
                                null
                        )
                );

        assertThat(slice.items())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.paymentId())
                            .isEqualTo(
                                    SECOND_PAYMENT_ID
                            );
                    assertThat(payment.status())
                            .isEqualTo(
                                    ObservedPaymentStatus.FAILED
                            );
                });
    }

    @Test
    void detailQueryRehydratesInstitutionsAndMaskedAccounts() {
        projectionService.observe(
                command(
                        FIRST_EVENT_ID,
                        FIRST_PAYMENT_ID,
                        "PAY-CUST-001",
                        ObservedPaymentStatus.DEBITED,
                        T0
                )
        );

        var detail =
                customerQueryRepository.findDetailById(
                        ObservedCustomerId.of(
                                CUSTOMER_ID
                        )
                );

        assertThat(detail).isPresent();
        assertThat(detail.orElseThrow()
                .observedCustomerId())
                .isEqualTo(
                        ObservedCustomerId.of(
                                CUSTOMER_ID
                        )
                );
        assertThat(detail.orElseThrow()
                .institutions())
                .isNotEmpty();
    }

    private static ObserveCustomerCommand command(
            UUID sourceEventId,
            UUID paymentId,
            String paymentReference,
            ObservedPaymentStatus status,
            Instant createdAt
    ) {
        return new ObserveCustomerCommand(
                sourceEventId,
                paymentId,
                paymentReference,
                "NIU-00000001",
                "ALICE CUSTOMER",
                "***1234",
                "a***@example.com",
                "LAREGIONALE",
                "v1:" + "a".repeat(64),
                "****1234",
                new BigDecimal("12500.00"),
                "XAF",
                status,
                status == ObservedPaymentStatus.FAILED
                        ? "BANK_REJECTED"
                        : null,
                createdAt,
                createdAt.plusSeconds(1),
                createdAt.plusSeconds(2),
                CORRELATION_ID.toString()
        );
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = {
            ObservedCustomerJpaEntity.class,
            ObservedCustomerInstitutionJpaEntity.class,
            ObservedAccountJpaEntity.class,
            ObservedPaymentJpaEntity.class,
            ProcessedObservationEventJpaEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            ObservedCustomerSpringDataRepository.class,
            ObservedPaymentSpringDataRepository.class,
            ProcessedObservationEventSpringDataRepository.class
    })
    static class TestApplication {

        @Bean
        ObservedCustomerDataProtector observedCustomerDataProtector() {
            return new AesGcmObservedCustomerDataProtector(
                    Base64.getEncoder()
                            .encodeToString(
                                    new byte[32]
                            )
            );
        }

        @Bean
        ObservedCustomerPersistenceMapper
        observedCustomerPersistenceMapper(
                ObservedCustomerDataProtector protector
        ) {
            return new ObservedCustomerPersistenceMapper(
                    protector
            );
        }

        @Bean
        ObservedCustomerRepository observedCustomerRepository(
                ObservedCustomerSpringDataRepository customers,
                ObservedPaymentSpringDataRepository payments,
                ProcessedObservationEventSpringDataRepository events,
                ObservedCustomerDataProtector protector,
                ObservedCustomerPersistenceMapper mapper
        ) {
            return new JpaObservedCustomerRepositoryAdapter(
                    customers,
                    payments,
                    events,
                    protector,
                    mapper
            );
        }

        @Bean
        ObservedPaymentRepository observedPaymentRepository(
                ObservedCustomerSpringDataRepository customers,
                ObservedPaymentSpringDataRepository payments,
                ProcessedObservationEventSpringDataRepository events,
                ObservedCustomerPersistenceMapper mapper
        ) {
            return new JpaObservedPaymentRepositoryAdapter(
                    customers,
                    payments,
                    events,
                    mapper
            );
        }

        @Bean
        ObservedCustomerQueryRowMapper
        observedCustomerQueryRowMapper(
                ObservedCustomerDataProtector protector
        ) {
            return new ObservedCustomerQueryRowMapper(
                    protector
            );
        }

        @Bean
        ObservedCustomerQueryRepository
        observedCustomerQueryRepository(
                EntityManager entityManager,
                ObservedCustomerDataProtector protector,
                ObservedCustomerQueryRowMapper mapper
        ) {
            return new JpaObservedCustomerQueryAdapter(
                    entityManager,
                    protector,
                    mapper
            );
        }

        @Bean
        ObservedCustomerPaymentQueryRepository
        observedCustomerPaymentQueryRepository(
                EntityManager entityManager,
                ObservedCustomerQueryRowMapper mapper
        ) {
            return new JpaObservedCustomerPaymentQueryAdapter(
                    entityManager,
                    mapper
            );
        }
    }
}

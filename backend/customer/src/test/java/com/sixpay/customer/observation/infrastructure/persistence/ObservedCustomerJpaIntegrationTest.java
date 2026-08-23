package com.sixpay.customer.observation.infrastructure.persistence;

import com.sixpay.customer.configuration
        .CustomerModuleConfiguration;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.application.port.output
        .ObservedCustomerIdGenerator;
import com.sixpay.customer.observation.application.port.output
        .ObservedCustomerRepository;
import com.sixpay.customer.observation.application.port.output
        .ObservedPaymentRepository;
import com.sixpay.customer.observation.application.service
        .ObservedCustomerProjectionService;
import com.sixpay.customer.observation.configuration
        .ObservedCustomerPersistenceConfiguration;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import com.sixpay.customer.observation.infrastructure.persistence.transaction
        .TransactionalObserveCustomerUseCase;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureClassifier;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure
        .DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes =
                ObservedCustomerJpaIntegrationTest
                        .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",

                "sixpay.customer.observation."
                        + "persistence.enabled=true",

                "sixpay.customer.observation."
                        + "audit.persistence.enabled=false",

                "sixpay.customer.observation."
                        + "query.enabled=false",

                "sixpay.customer.verification."
                        + "banking.enabled=false",

                "sixpay.customer.observation.persistence."
                        + "max-optimistic-attempts=3",

                "sixpay.customer.observation.resilience."
                        + "max-attempts=3",

                "sixpay.customer.observation.resilience."
                        + "initial-backoff=1ms",

                "sixpay.customer.observation.resilience."
                        + "max-backoff=5ms",

                "sixpay.customer.observation.resilience."
                        + "multiplier=2",

                "sixpay.customer.observation.resilience."
                        + "jitter=0",

                "sixpay.customer.observation.persistence."
                        + "protection-key-base64="
                        + "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3"
                        + "ODlhYmNkZWY="
        }
)
class ObservedCustomerJpaIntegrationTest {

    private static final Instant FIRST =
            Instant.parse(
                    "2026-08-03T20:00:00Z"
            );

    private static final UUID FIXED_CUSTOMER_ID =
            UUID.fromString(
                    "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
            );

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    "postgres:17-alpine"
            )
                    .withDatabaseName(
                            "sixpay_customer"
                    )
                    .withUsername(
                            "sixpay"
                    )
                    .withPassword(
                            "sixpay"
                    );

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
                "spring.jpa.open-in-view",
                () -> false
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ObserveCustomerUseCase useCase;

    @org.springframework.beans.factory.annotation.Autowired
    private ObservedCustomerRepository customerRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ObservedPaymentRepository paymentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ObservedCustomerIdGenerator idGenerator;

    @org.springframework.beans.factory.annotation.Autowired
    private PlatformTransactionManager transactionManager;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute(
                """
                TRUNCATE TABLE
                    customer_observation_processed_event,
                    customer_observed_payment,
                    customer_observed_account,
                    customer_observed_institution,
                    customer_observed_customer
                RESTART IDENTITY CASCADE
                """
        );
    }

    @Test
    void flywayCreatesCompleteProjectionAndLookupUsesExactNiuHash() {
        ObserveCustomerCommand command =
                command(
                        event("11111111"),
                        payment("aaaaaaaa"),
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST.plusSeconds(1)
                );

        ObserveCustomerResult result =
                useCase.observe(command);

        assertEquals(
                ObserveCustomerResult.Disposition.APPLIED,
                result.disposition()
        );

        assertTrue(
                customerRepository
                        .findByNormalizedNiu("M0123456")
                        .isPresent()
        );

        assertTrue(
                customerRepository
                        .findByNormalizedNiu("M9999999")
                        .isEmpty()
        );

        assertEquals(
                1,
                count("customer_observed_customer")
        );

        assertEquals(
                1,
                count("customer_observed_institution")
        );

        assertEquals(
                1,
                count("customer_observed_account")
        );

        assertEquals(
                1,
                count("customer_observed_payment")
        );

        assertEquals(
                1,
                count("customer_observation_processed_event")
        );

        String niuProtected =
                jdbc.queryForObject(
                        """
                        SELECT niu_protected
                        FROM customer_observed_customer
                        """,
                        String.class
                );

        String legalNameProtected =
                jdbc.queryForObject(
                        """
                        SELECT legal_name_protected
                        FROM customer_observed_customer
                        """,
                        String.class
                );

        String niuHash =
                jdbc.queryForObject(
                        """
                        SELECT niu_search_hash
                        FROM customer_observed_customer
                        """,
                        String.class
                );

        assertFalse(
                niuProtected.contains("M0123456")
        );

        assertFalse(
                legalNameProtected.contains(
                        "Société ABC SARL"
                )
        );

        assertEquals(
                64,
                niuHash.length()
        );

        assertFalse(
                niuHash.equals("M0123456")
        );
    }

    @Test
    void replayIsIdempotentAndWatermarkUpdatesForNewEvent() {
        ObserveCustomerCommand first =
                command(
                        event("11111111"),
                        payment("aaaaaaaa"),
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST.plusSeconds(1)
                );

        ObserveCustomerResult applied =
                useCase.observe(first);

        ObserveCustomerResult replayed =
                useCase.observe(first);

        assertEquals(
                ObserveCustomerResult.Disposition.APPLIED,
                applied.disposition()
        );

        assertEquals(
                ObserveCustomerResult.Disposition.REPLAYED,
                replayed.disposition()
        );

        assertEquals(
                1,
                count("customer_observation_processed_event")
        );

        UUID secondEvent =
                event("22222222");

        useCase.observe(
                command(
                        secondEvent,
                        first.paymentId(),
                        ObservedPaymentStatus.DEBITED,
                        null,
                        FIRST.plusSeconds(30),
                        FIRST.plusSeconds(31)
                )
        );

        assertEquals(
                secondEvent.toString(),
                jdbc.queryForObject(
                        """
                        SELECT source_event_watermark
                        FROM customer_observed_customer
                        """,
                        String.class
                )
        );

        assertEquals(
                2L,
                jdbc.queryForObject(
                        """
                        SELECT projection_version
                        FROM customer_observed_customer
                        """,
                        Long.class
                )
        );

        assertEquals(
                1L,
                jdbc.queryForObject(
                        """
                        SELECT successful_payments
                        FROM customer_observed_customer
                        """,
                        Long.class
                )
        );
    }

    @Test
    void outOfOrderEventsSurviveRestartAndKeepLatestPaymentStatus() {
        UUID paymentId =
                payment("aaaaaaaa");

        useCase.observe(
                command(
                        event("11111111"),
                        paymentId,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST.plusSeconds(1)
                )
        );

        useCase.observe(
                command(
                        event("22222222"),
                        paymentId,
                        ObservedPaymentStatus.DEBITED,
                        null,
                        FIRST.plusSeconds(60),
                        FIRST.plusSeconds(61)
                )
        );

        ObserveCustomerResult stale =
                useCase.observe(
                        command(
                                event("33333333"),
                                paymentId,
                                ObservedPaymentStatus.REJECTED,
                                "ACCOUNT_NOT_FOUND",
                                FIRST.plusSeconds(20),
                                FIRST.plusSeconds(62)
                        )
                );

        assertEquals(
                ObserveCustomerResult
                        .Disposition
                        .IGNORED_STALE,
                stale.disposition()
        );

        var restored =
                customerRepository
                        .findByNormalizedNiu("M0123456")
                        .orElseThrow();

        assertEquals(
                ObservedPaymentStatus.DEBITED,
                restored.lastPaymentStatus()
        );

        assertEquals(
                1,
                restored.successfulPayments()
        );

        assertEquals(
                0,
                restored.failedPayments()
        );

        assertEquals(
                3,
                restored
                        .appliedSourceEventIds()
                        .size()
        );
    }

    @Test
    void transactionRollsBackProjectionWhenLinkedPaymentWriteFails() {
        ObservedPaymentRepository failingPayments =
                (
                        customerId,
                        sourceEventId,
                        payment,
                        watermark,
                        observedAt
                ) -> {
                    throw new IllegalStateException(
                            "simulated linked-payment failure"
                    );
                };

        ObserveCustomerUseCase service =
                new ObservedCustomerProjectionService(
                        customerRepository,
                        failingPayments,
                        idGenerator
                );

        ObserveCustomerUseCase transactional =
                new TransactionalObserveCustomerUseCase(
                        service,
                        transactionManager,
                        1
                );

        assertThrows(
                IllegalStateException.class,
                () -> transactional.observe(
                        command(
                                event("11111111"),
                                payment("aaaaaaaa"),
                                ObservedPaymentStatus.RECEIVED,
                                null,
                                FIRST,
                                FIRST.plusSeconds(1)
                        )
                )
        );

        assertEquals(
                0,
                count("customer_observed_customer")
        );

        assertEquals(
                0,
                count("customer_observed_payment")
        );
    }

    @Test
    void concurrentReplayProducesOneProjectionAndOneProcessedEvent()
            throws Exception {

        ObserveCustomerCommand command =
                command(
                        event("11111111"),
                        payment("aaaaaaaa"),
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST.plusSeconds(1)
                );

        try (var executor =
                     Executors.newFixedThreadPool(2)) {

            List<Callable<ObserveCustomerResult>> tasks =
                    List.of(
                            () -> useCase.observe(command),
                            () -> useCase.observe(command)
                    );

            List<ObserveCustomerResult> results =
                    executor.invokeAll(tasks)
                            .stream()
                            .map(future -> {
                                try {
                                    return future.get();
                                } catch (Exception exception) {
                                    throw new IllegalStateException(
                                            exception
                                    );
                                }
                            })
                            .toList();

            assertEquals(
                    1,
                    results.stream()
                            .filter(result ->
                                    result.disposition()
                                            == ObserveCustomerResult
                                            .Disposition.APPLIED
                            )
                            .count()
            );

            assertEquals(
                    1,
                    results.stream()
                            .filter(result ->
                                    result.disposition()
                                            == ObserveCustomerResult
                                            .Disposition.REPLAYED
                            )
                            .count()
            );
        }

        assertEquals(
                1,
                count("customer_observed_customer")
        );

        assertEquals(
                1,
                count("customer_observed_payment")
        );

        assertEquals(
                1,
                count("customer_observation_processed_event")
        );
    }

    @Test
    void sourceEventPrimaryKeyRejectsDuplicateRawInsert() {
        ObserveCustomerCommand command =
                command(
                        event("11111111"),
                        payment("aaaaaaaa"),
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST.plusSeconds(1)
                );

        useCase.observe(command);

        UUID customerId =
                jdbc.queryForObject(
                        """
                        SELECT observed_customer_id
                        FROM customer_observed_customer
                        """,
                        UUID.class
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(
                        """
                        INSERT INTO
                            customer_observation_processed_event (
                                source_event_id,
                                observed_customer_id,
                                source_event_watermark,
                                observed_at,
                                processed_at
                            )
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        command.sourceEventId(),
                        customerId,
                        command
                                .sourceEventId()
                                .toString(),
                        Timestamp.from(
                                command.observedAt()
                        ),
                        Timestamp.from(
                                command.observedAt()
                        )
                )
        );
    }

    private int count(
            String table
    ) {
        Integer value =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM " + table,
                        Integer.class
                );

        return value == null
                ? 0
                : value;
    }

    private static ObserveCustomerCommand command(
            UUID sourceEventId,
            UUID paymentId,
            ObservedPaymentStatus status,
            String failureCode,
            Instant paymentUpdatedAt,
            Instant observedAt
    ) {
        return new ObserveCustomerCommand(
                sourceEventId,
                paymentId,
                "PAY-" + paymentId,
                "M0123456",
                "Société ABC SARL",
                "***-***-1234",
                "a***@example.com",
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("15000.00"),
                "XAF",
                status,
                failureCode,
                FIRST,
                paymentUpdatedAt,
                observedAt,
                "c74e165f-df46-463e-a520-188e6df3e5ae"
        );
    }

    private static UUID event(
            String prefix
    ) {
        return UUID.fromString(
                prefix
                        + "-1111-4111-8111-111111111111"
        );
    }

    private static UUID payment(
            String prefix
    ) {
        return UUID.fromString(
                prefix
                        + "-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataJpaRepositoriesAutoConfiguration.class,
                    CustomerModuleConfiguration.class
            }
    )
    @Import({
            ObservedCustomerPersistenceConfiguration.class,
            IntegrationTestConfiguration.class
    })
    static class TestApplication {
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class IntegrationTestConfiguration {

        @Bean
        @Primary
        ObservedCustomerIdGenerator
        testObservedCustomerIdGenerator() {
            return () -> ObservedCustomerId.of(
                    FIXED_CUSTOMER_ID
            );
        }

        @Bean
        ObservedCustomerProjectionFailureClassifier
        observedCustomerProjectionFailureClassifier() {
            return new ObservedCustomerProjectionFailureClassifier();
        }

        @Bean
        ObservedCustomerProjectionRetryPolicy
        observedCustomerProjectionRetryPolicy() {
            return new ObservedCustomerProjectionRetryPolicy(
                    3,
                    Duration.ofMillis(1),
                    Duration.ofMillis(5),
                    2.0D,
                    0.0D,
                    duration -> {
                        // Aucun délai réel dans le test d’intégration.
                    },
                    () -> 0.5D
            );
        }

        @Bean
        Clock testClock() {
            return Clock.fixed(
                    FIRST.plusSeconds(120),
                    ZoneOffset.UTC
            );
        }

        @Bean
        JwtDecoder testJwtDecoder() {
            return token ->
                    Jwt.withTokenValue(token)
                            .header(
                                    "alg",
                                    "none"
                            )
                            .claims(claims -> {
                                claims.put(
                                        "sub",
                                        "integration-test"
                                );

                                claims.put(
                                        "scope",
                                        "customer:read "
                                                + "customer:write"
                                );
                            })
                            .issuedAt(
                                    Instant.parse(
                                            "2026-08-03T20:00:00Z"
                                    )
                            )
                            .expiresAt(
                                    Instant.parse(
                                            "2030-08-03T20:00:00Z"
                                    )
                            )
                            .build();
        }
    }
}
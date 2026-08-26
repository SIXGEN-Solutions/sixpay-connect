package com.sixpay.customer.observation.infrastructure.audit;

import com.sixpay.customer.configuration.CustomerModuleConfiguration;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditContext;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import com.sixpay.customer.observation.configuration
        .ObservedCustomerAuditPersistenceConfiguration;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure
        .DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = ObservedCustomerAuditJpaIntegrationTest
                .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "sixpay.customer.observation.persistence.enabled=false",
                "sixpay.customer.observation.audit.persistence.enabled=true",
                "sixpay.customer.observation.query.enabled=false",
                "sixpay.customer.verification.banking.enabled=false"
        }
)
class ObservedCustomerAuditJpaIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("sixpay_customer")
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
                "spring.jpa.open-in-view",
                () -> false
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ObservedCustomerAuditPort auditPort;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute(
                "TRUNCATE TABLE customer_observation_audit"
        );
    }

    @Test
    void appendsAuditWithoutProjectionForeignKey() {
        UUID auditId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        UUID customerId = UUID.fromString(
                "44444444-4444-4444-8444-444444444444"
        );

        auditPort.append(
                queryRecord(
                        auditId,
                        customerId
                )
        );

        assertEquals(
                1,
                countAuditRows()
        );

        assertEquals(
                customerId,
                jdbc.queryForObject(
                        """
                        SELECT observed_customer_id
                        FROM customer_observation_audit
                        WHERE audit_id = ?
                        """,
                        UUID.class,
                        auditId
                )
        );
    }

    @Test
    void duplicateAuditIdCannotBecomeAnUpdate() {
        ObservedCustomerAuditRecord record =
                queryRecord(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                );

        auditPort.append(record);

        assertThrows(
                IllegalStateException.class,
                () -> auditPort.append(record)
        );

        assertEquals(
                1,
                countAuditRows()
        );
    }

    @Test
    void databaseRejectsUpdateAndDelete() {
        UUID auditId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );

        auditPort.append(
                queryRecord(
                        auditId,
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                )
        );

        assertThrows(
                DataAccessException.class,
                () -> jdbc.update(
                        """
                        UPDATE customer_observation_audit
                        SET reason_code = 'CHANGED'
                        WHERE audit_id = ?
                        """,
                        auditId
                )
        );

        assertThrows(
                DataAccessException.class,
                () -> jdbc.update(
                        """
                        DELETE FROM customer_observation_audit
                        WHERE audit_id = ?
                        """,
                        auditId
                )
        );

        assertEquals(
                1,
                countAuditRows()
        );
    }

    @Test
    void requiredIndexesExist() {
        Integer indexCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'customer_observation_audit'
                  AND indexname IN (
                      'idx_customer_observation_audit_customer',
                      'idx_customer_observation_audit_source_event',
                      'idx_customer_observation_audit_correlation',
                      'idx_customer_observation_audit_occurred'
                  )
                """,
                Integer.class
        );

        assertEquals(
                4,
                indexCount
        );
    }

    @Test
    void auditTableHasNoForeignKeyToProjection() {
        Integer foreignKeyCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'customer_observation_audit'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        );

        assertEquals(
                0,
                foreignKeyCount
        );
    }

    private int countAuditRows() {
        Integer result = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM customer_observation_audit
                """,
                Integer.class
        );

        return result == null
                ? 0
                : result;
    }

    private static ObservedCustomerAuditRecord queryRecord(
            UUID auditId,
            UUID customerId
    ) {
        return ObservedCustomerAuditRecord.query(
                auditId,
                ObservedCustomerAuditAction.QUERY_DETAIL_READ,
                ObservedCustomerAuditOutcome.SUCCEEDED,
                ObservedCustomerId.of(customerId),
                new ObservedCustomerAuditContext(
                        "service-account:customer",
                        "55555555-5555-4555-8555-555555555555"
                ),
                Instant.parse(
                        "2026-08-05T15:00:00Z"
                ),
                null
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataJpaRepositoriesAutoConfiguration.class,
                    CustomerModuleConfiguration.class
            }
    )
    @Import(
            ObservedCustomerAuditPersistenceConfiguration.class
    )
    static class TestApplication {
    }
}
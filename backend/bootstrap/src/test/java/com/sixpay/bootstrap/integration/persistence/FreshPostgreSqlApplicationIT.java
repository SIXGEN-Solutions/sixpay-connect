package com.sixpay.bootstrap.integration.persistence;

import com.sixpay.SixpayApplication;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = SixpayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.default_schema=sixpay",

                "spring.flyway.enabled=true",
                "spring.flyway.schemas=sixpay",
                "spring.flyway.default-schema=sixpay",
                "spring.flyway.create-schemas=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.validate-on-migrate=true",
                "spring.flyway.baseline-on-migrate=false",
                "spring.flyway.clean-disabled=true",
                "spring.flyway.out-of-order=true",

                "sixpay.payment.callback.enabled=false",
                "sixpay.customer.verification.banking.enabled=false",
                "sixpay.notification.operational.email.enabled=false",
                "sixpay.notification.operational.retry.enabled=false",
                "sixpay.notification.operational.operations.retention-enabled=false",
                "sixpay.notification.operational.operations.metrics-enabled=false",

                "sixpay.accounting.batch.cutoff-zone=Africa/Douala",
                "sixpay.accounting.batch.cutoff-time=23:59"
        }
)
@ActiveProfiles("standalone")
@ContextConfiguration(
        initializers =
                FreshPostgreSqlApplicationIT
                        .EmptySchemaAssertionInitializer.class
)
@Testcontainers(disabledWithoutDocker = true)
class FreshPostgreSqlApplicationIT {

    private static final String SCHEMA = "sixpay";

    private static final Set<String> REQUIRED_BASELINES =
            Set.of(
                    "100",
                    "200",
                    "300",
                    "400",
                    "500",
                    "600",
                    "700",
                    "800"
            );

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("sixpay_fs_2_3_7")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                () -> POSTGRES.getJdbcUrl()
                        + "?currentSchema="
                        + SCHEMA
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
                "spring.datasource.hikari.schema",
                () -> SCHEMA
        );
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void freshPostgreSqlBootsCanonicalApplication()
            throws Exception {

        assertTrue(
                applicationContext
                        instanceof ConfigurableApplicationContext
        );

        assertTrue(
                ((ConfigurableApplicationContext)
                        applicationContext)
                        .isActive(),
                "SIXPAY application context must be active"
        );

        assertNotNull(
                entityManagerFactory,
                "EntityManagerFactory must exist after "
                        + "Flyway + Hibernate validate"
        );

        Set<String> appliedVersions =
                Arrays.stream(
                                flyway.info()
                                        .applied()
                        )
                        .filter(info ->
                                info.getVersion() != null
                        )
                        .map(info ->
                                info.getVersion()
                                        .getVersion()
                        )
                        .collect(Collectors.toSet());

        assertTrue(
                appliedVersions.containsAll(
                        REQUIRED_BASELINES
                ),
                () -> "Missing canonical baselines. Applied: "
                        + appliedVersions
        );

        assertFalse(
                appliedVersions.stream()
                        .anyMatch(version ->
                                version.startsWith("2026")
                        ),
                () -> "Historical migration applied: "
                        + appliedVersions
        );

        assertSchemaExistsAfterStartup();
        assertFlywayHistoryExists();
    }

    private void assertSchemaExistsAfterStartup()
            throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()
                        );
                ResultSet schemas =
                        connection.getMetaData()
                                .getSchemas(
                                        null,
                                        SCHEMA
                                )
        ) {
            assertTrue(
                    schemas.next(),
                    "Flyway must create schema sixpay"
            );
        }
    }

    private void assertFlywayHistoryExists()
            throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()
                        );
                ResultSet tables =
                        connection.getMetaData()
                                .getTables(
                                        null,
                                        SCHEMA,
                                        "flyway_schema_history",
                                        new String[]{"TABLE"}
                                )
        ) {
            assertTrue(
                    tables.next(),
                    "sixpay.flyway_schema_history must exist"
            );
        }
    }

    static class EmptySchemaAssertionInitializer
            implements ApplicationContextInitializer<
            ConfigurableApplicationContext> {

        @Override
        public void initialize(
                ConfigurableApplicationContext context
        ) {

            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    POSTGRES.getJdbcUrl(),
                                    POSTGRES.getUsername(),
                                    POSTGRES.getPassword()
                            );
                    ResultSet schemas =
                            connection.getMetaData()
                                    .getSchemas(
                                            null,
                                            SCHEMA
                                    )
            ) {
                assertFalse(
                        schemas.next(),
                        "FS-2.3.7 requires schema sixpay "
                                + "to be absent before "
                                + "Spring/Flyway starts"
                );
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Unable to prove empty PostgreSQL "
                                + "before application startup",
                        exception
                );
            }

            ConfigurableEnvironment environment =
                    context.getEnvironment();

            assertFalse(
                    environment.getProperty(
                            "spring.flyway.baseline-on-migrate",
                            Boolean.class,
                            false
                    ),
                    "baseline-on-migrate workaround is forbidden"
            );
        }
    }
}

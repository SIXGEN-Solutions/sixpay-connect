package com.sixpay.tests.assembled;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8.3.1 — full backend assembly gate.
 *
 * <p>This test intentionally compiles without a direct dependency on
 * {@code com.sixpay.SixpayApplication}. The full production module graph is
 * contributed by the Maven {@code assembled-tests} profile. This prevents
 * bootstrap from contaminating focused cross-module tests.</p>
 */
@SpringBootTest(
        classes = AssembledApplicationContextIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("assembled-test")
@Testcontainers
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
class AssembledApplicationContextIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            )
                    .withDatabaseName("sixpay_assembled")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

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
    private ApplicationContext context;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void assembledApplicationContextStarts() {
        assertThat(context).isNotNull();
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void allClasspathFlywayMigrationsCoexistInSharedSchema() {
        assertThat(flyway.info().applied())
                .as("assembled application must apply module migrations")
                .isNotEmpty();

        Integer historyRows = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                  FROM sixpay.flyway_schema_history
                 WHERE success = TRUE
                """,
                Integer.class
        );

        assertThat(historyRows)
                .isNotNull()
                .isGreaterThan(0);
    }

    @Test
    void principalModuleAutoConfigurationsArePresent() {
        List<String> requiredConfigurationTypes = List.of(
                "com.sixpay.partner.configuration.PartnerModuleConfiguration",
                "com.sixpay.customer.configuration.CustomerModuleConfiguration",
                "com.sixpay.payment.configuration.PaymentModuleConfiguration",
                "com.sixpay.accounting.configuration.AccountingModuleConfiguration",
                "com.sixpay.security.configuration.SixpaySecurityAutoConfiguration",
                "com.sixpay.notification.configuration.NotificationPersistenceAutoConfiguration"
        );

        for (String typeName : requiredConfigurationTypes) {
            assertConfigurationBeanPresent(typeName);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertConfigurationBeanPresent(
            String typeName
    ) {
        try {
            Class type = Class.forName(typeName);

            assertThat(context.getBeansOfType(type))
                    .as(
                            typeName
                                    + " must participate in the assembled context"
                    )
                    .isNotEmpty();
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required assembled module type is absent: "
                            + typeName,
                    exception
            );
        }
    }

    /**
     * Mirrors the production root scan without importing the bootstrap main
     * class at compile time. Test packages are deliberately excluded by using
     * explicit production package roots.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {
            "com.sixpay.common",
            "com.sixpay.partner",
            "com.sixpay.customer",
            "com.sixpay.payment",
            "com.sixpay.accounting",
            "com.sixpay.reporting",
            "com.sixpay.notification",
            "com.sixpay.administration",
            "com.sixpay.security",
            "com.sixpay.integration"
    })
    static class TestApplication {
    }
}

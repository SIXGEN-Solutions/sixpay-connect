package com.sixpay.bootstrap.integration.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class FlywayModuleAssemblyIT {

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

    private static final Set<String> REPRESENTATIVE_TABLES =
            Set.of(
                    "partners",
                    "customer_management_customer",
                    "payments",
                    "accounting_batches",
                    "reporting_payment_audit_evidence",
                    "operational_notification_deliveries",
                    "security_user_accounts",
                    "operational_incident"
            );

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("sixpay_fs_2_3_6")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

    @Test
    void bootstrapClasspathAssemblesAllDomainMigrations()
            throws Exception {

        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .outOfOrder(true)
                .load();

        flyway.migrate();

        MigrationInfo[] applied = flyway.info().applied();

        Set<String> appliedVersions =
                Arrays.stream(applied)
                        .filter(info -> info.getVersion() != null)
                        .map(info -> info.getVersion().getVersion())
                        .collect(Collectors.toSet());

        assertTrue(
                appliedVersions.containsAll(REQUIRED_BASELINES),
                () -> "Missing canonical baselines. Applied: "
                        + appliedVersions
        );

        assertFalse(
                appliedVersions.stream()
                        .anyMatch(version ->
                                version.startsWith("2026")
                        ),
                () -> "Historical pre-baseline migration "
                        + "must not be applied: "
                        + appliedVersions
        );

        assertSchemaHistoryLivesInSixpay();
        assertRepresentativeDomainTablesExist();
    }

    private void assertSchemaHistoryLivesInSixpay()
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
                    "Flyway schema history must live in schema sixpay"
            );
        }
    }

    private void assertRepresentativeDomainTablesExist()
            throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                POSTGRES.getJdbcUrl(),
                                POSTGRES.getUsername(),
                                POSTGRES.getPassword()
                        )
        ) {
            for (String table : REPRESENTATIVE_TABLES) {
                try (
                        ResultSet tables =
                                connection.getMetaData()
                                        .getTables(
                                                null,
                                                SCHEMA,
                                                table,
                                                new String[]{"TABLE"}
                                        )
                ) {
                    assertTrue(
                            tables.next(),
                            () -> "Missing assembled domain table "
                                    + SCHEMA
                                    + "."
                                    + table
                    );
                }
            }
        }
    }
}

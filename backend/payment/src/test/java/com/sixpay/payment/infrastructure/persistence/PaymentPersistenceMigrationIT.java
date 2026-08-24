package com.sixpay.payment.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PaymentPersistenceMigrationIT {

    @Container
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:15-alpine")
            );

    @Test
    void createsPaymentTableAndRequiredConstraints()
            throws Exception {

        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRESQL.getJdbcUrl(),
                        POSTGRESQL.getUsername(),
                        POSTGRESQL.getPassword()
                )
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();

        assertPaymentBaselineWasApplied(flyway);
        assertHistoricalPaymentMigrationsAreAbsent(flyway);

        assertPaymentsTableExistsWithRequiredColumns();
        assertSourceExternalReferenceUniqueIndexExists();
    }

    private void assertPaymentBaselineWasApplied(
            Flyway flyway
    ) {

        boolean paymentBaselineApplied =
                Arrays.stream(
                                flyway.info()
                                        .applied()
                        )
                        .anyMatch(
                                migration ->
                                        "300".equals(
                                                migration
                                                        .getVersion()
                                                        .getVersion()
                                        )
                                                && "payment baseline"
                                                .equals(
                                                        migration
                                                                .getDescription()
                                                )
                        );

        assertTrue(
                paymentBaselineApplied,
                "Canonical Payment baseline V300 "
                        + "must be applied"
        );
    }

    private void assertHistoricalPaymentMigrationsAreAbsent(
            Flyway flyway
    ) {

        boolean historicalPaymentMigrationApplied =
                Arrays.stream(
                                flyway.info()
                                        .applied()
                        )
                        .anyMatch(
                                migration -> {
                                    if (migration.getVersion() == null) {
                                        return false;
                                    }

                                    String version =
                                            migration
                                                    .getVersion()
                                                    .getVersion();

                                    return version.startsWith(
                                            "20260801"
                                    )
                                            || version.equals(
                                            "202608071900"
                                    );
                                }
                        );

        assertFalse(
                historicalPaymentMigrationApplied,
                "Historical Payment migrations "
                        + "must not survive FS-2.3 squash"
        );
    }

    private void assertPaymentsTableExistsWithRequiredColumns()
            throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                POSTGRESQL.getJdbcUrl(),
                                POSTGRESQL.getUsername(),
                                POSTGRESQL.getPassword()
                        );

                ResultSet columns =
                        connection
                                .getMetaData()
                                .getColumns(
                                        null,
                                        null,
                                        "payments",
                                        null
                                )
        ) {

            int count = 0;

            while (columns.next()) {
                count++;
            }

            assertTrue(
                    count >= 15,
                    "payments table must expose "
                            + "the canonical Payment columns"
            );
        }
    }

    private void assertSourceExternalReferenceUniqueIndexExists()
            throws Exception {

        try (
                Connection connection =
                        DriverManager.getConnection(
                                POSTGRESQL.getJdbcUrl(),
                                POSTGRESQL.getUsername(),
                                POSTGRESQL.getPassword()
                        );

                ResultSet indexes =
                        connection
                                .getMetaData()
                                .getIndexInfo(
                                        null,
                                        null,
                                        "payments",
                                        false,
                                        false
                                )
        ) {

            boolean sourceExternalUnique = false;

            while (indexes.next()) {
                String name =
                        indexes.getString(
                                "INDEX_NAME"
                        );

                if (
                        "uk_payments_source_external_reference"
                                .equalsIgnoreCase(name)
                ) {
                    sourceExternalUnique = true;
                }
            }

            assertTrue(
                    sourceExternalUnique,
                    "Canonical Payment baseline "
                            + "must create "
                            + "uk_payments_source_external_reference"
            );
        }
    }
}
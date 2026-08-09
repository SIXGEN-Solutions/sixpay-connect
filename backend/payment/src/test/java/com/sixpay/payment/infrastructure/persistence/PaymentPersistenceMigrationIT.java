package com.sixpay.payment.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PaymentPersistenceMigrationIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>(
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

        assertEquals(6, flyway.migrate().migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(
                    POSTGRESQL.getJdbcUrl(),
                    POSTGRESQL.getUsername(),
                    POSTGRESQL.getPassword()
             );
             ResultSet columns = connection.getMetaData()
                     .getColumns(
                             null,
                             null,
                             "payments",
                             null
                     )) {

            int count = 0;
            while (columns.next()) {
                count++;
            }

            assertTrue(count >= 15);
        }

        try (Connection connection = DriverManager.getConnection(
                    POSTGRESQL.getJdbcUrl(),
                    POSTGRESQL.getUsername(),
                    POSTGRESQL.getPassword()
             );
             ResultSet indexes = connection.getMetaData()
                     .getIndexInfo(
                             null,
                             null,
                             "payments",
                             false,
                             false
                     )) {

            boolean sourceExternalUnique = false;

            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if ("uk_payments_source_external_reference"
                        .equalsIgnoreCase(name)) {
                    sourceExternalUnique = true;
                }
            }

            assertTrue(sourceExternalUnique);
        }
    }
}

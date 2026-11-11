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
import java.util.UUID;
import java.util.Locale;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertPartnerExternalReferenceUniqueIndexExists();
    }

    @Test
    void paymentStateVersionConstraintAcceptsCurrentVersionAndRejectsMalformedValues()
            throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(),
                        POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> migration.getVersion() != null
                        && "301".equals(migration.getVersion().getVersion())),
                "Option B corrective migration V301 must be applied");

        // The current Java writer's document version must be accepted by the DB.
        assertStatePayloadInsertAccepted("{\"schemaVersion\":"
                + PaymentStateDocument.CURRENT_SCHEMA_VERSION + "}");
        assertStatePayloadInsertAccepted("{\"schemaVersion\":1}");

        // SQL remains structural. Java rejects future versions on reconstitution.
        assertStatePayloadInsertAccepted("{\"schemaVersion\":999}");
        assertStatePayloadInsertRejected("{}");
        assertStatePayloadInsertRejected("{\"schemaVersion\":null}");
        assertStatePayloadInsertRejected("{\"schemaVersion\":0}");
        assertStatePayloadInsertRejected("{\"schemaVersion\":-1}");
        assertStatePayloadInsertRejected("{\"schemaVersion\":7.5}");
        assertStatePayloadInsertRejected("{\"schemaVersion\":\"7\"}");
    }

    private void assertStatePayloadInsertAccepted(String payload) throws Exception {
        insertPaymentStatePayload(payload);
    }

    private void assertStatePayloadInsertRejected(String payload) {
        assertThrows(SQLException.class, () -> insertPaymentStatePayload(payload),
                "Invalid JSON schema version must be rejected: " + payload);
    }

    /** Roll back each probe; it must never leave test Payments behind. */
    private void insertPaymentStatePayload(String payload) throws Exception {
        UUID id = UUID.randomUUID();
        String publicReference = "PAY-" + id.toString().replace("-", "")
                .substring(0, 26).toUpperCase(Locale.ROOT);
        String sql = """
                INSERT INTO payments (
                    payment_id, public_payment_reference, payment_source,
                    external_payment_reference, external_subscription_reference,
                    requested_amount, requested_currency, status,
                    business_version, received_at, updated_at,
                    state_payload, persistence_version
                ) VALUES (?, ?, 'TRESOR_PAY', ?, 'SUB-TEST',
                    1000, 'XAF', 'RECEIVED', 1,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CAST(? AS jsonb), 0)
                """;
        try (Connection connection = DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword())) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, id);
                    statement.setString(2, publicReference);
                    statement.setString(3, "TEST-E2E-" + id);
                    statement.setString(4, payload);
                    statement.executeUpdate();
                }
            } finally {
                connection.rollback();
            }
        }
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

    private void assertPartnerExternalReferenceUniqueIndexExists()
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

            boolean partnerExternalUnique = false;

            while (indexes.next()) {
                String name =
                        indexes.getString(
                                "INDEX_NAME"
                        );

                if (
                        "uk_payments_partner_external_reference"
                                .equalsIgnoreCase(name)
                ) {
                    partnerExternalUnique = true;
                }
            }

            assertTrue(
                    partnerExternalUnique,
                    "Canonical Payment baseline "
                            + "must create "
                            + "uk_payments_partner_external_reference"
            );
        }
    }
}
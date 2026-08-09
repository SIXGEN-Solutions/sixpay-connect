package com.sixpay.reporting.integration;

import com.sixpay.reporting.application.exception
        .AuditExportConflictException;
import com.sixpay.reporting.application.query
        .RequestPaymentAuditExportCommand;
import com.sixpay.reporting.domain.model.AuditExportFormat;
import com.sixpay.reporting.infrastructure.export
        .JdbcAuditExportJobStore;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class PaymentAuditExportIdempotencyIT {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void sameKeyIsStableAndDifferentFingerprintConflicts()
            throws Exception {

        try (Connection connection =
                     postgres.createConnection("")) {

            executeMigration(
                    connection,
                    "V202608072058__create_"
                            + "reporting_payment_audit_projection.sql"
            );

            executeMigration(
                    connection,
                    "V202608072120__create_"
                            + "reporting_audit_export.sql"
            );

            JdbcAuditExportJobStore store =
                    new JdbcAuditExportJobStore(
                            new NamedParameterJdbcTemplate(
                                    new SingleConnectionDataSource(
                                            connection,
                                            true
                                    )
                            )
                    );

            RequestPaymentAuditExportCommand command =
                    command("Purpose for regulatory evidence");

            Instant requestedAt =
                    Instant.parse("2026-08-07T21:00:00Z");
            Instant expiresAt =
                    Instant.parse("2026-08-07T22:00:00Z");

            var first = store.accept(
                    command,
                    "fingerprint-a",
                    requestedAt,
                    expiresAt
            );
            var replay = store.accept(
                    command,
                    "fingerprint-a",
                    requestedAt,
                    expiresAt
            );

            assertTrue(first.newlyCreated());
            assertFalse(replay.newlyCreated());
            assertEquals(
                    first.job().exportId(),
                    replay.job().exportId()
            );

            assertThrows(
                    AuditExportConflictException.class,
                    () -> store.accept(
                            command(
                                    "Different regulatory purpose"
                            ),
                            "fingerprint-b",
                            requestedAt,
                            expiresAt
                    )
            );
        }
    }

    private static void executeMigration(
            Connection connection,
            String migration
    ) {
        ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource(
                        "db/migration/" + migration
                )
        );
    }

    private static RequestPaymentAuditExportCommand command(
            String purpose
    ) {
        return new RequestPaymentAuditExportCommand(
                "idem-001",
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-07T20:00:00Z"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                purpose,
                AuditExportFormat.CSV,
                "audit-user",
                UUID.randomUUID()
        );
    }
}

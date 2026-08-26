package com.sixpay.reporting.integration;

import com.sixpay.reporting.application.query.AuditSearchCriteria;
import com.sixpay.reporting.domain.model.AuditSort;
import com.sixpay.reporting.infrastructure.query
        .PaymentAuditProjectionReadAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class PaymentAuditPersistenceIT {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void auditProjectionSupportsStableSearch()
            throws Exception {

        try (Connection connection =
                     postgres.createConnection("")) {

            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource(
                            "db/migration/"
                                    + "V500__reporting_baseline.sql"
                    )
            );

            NamedParameterJdbcTemplate jdbc =
                    new NamedParameterJdbcTemplate(
                            new SingleConnectionDataSource(
                                    connection,
                                    true
                            )
                    );

            UUID evidenceId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            jdbc.getJdbcTemplate().update(
                    """
                    INSERT INTO reporting_payment_audit_evidence (
                        evidence_id,
                        payment_id,
                        category,
                        event_type,
                        timeline_result,
                        actor_type,
                        actor_id,
                        action,
                        target_type,
                        target_id,
                        audit_result,
                        reason_code,
                        correlation_id,
                        source_system,
                        aggregate_version,
                        integrity_scheme,
                        integrity_value,
                        occurred_at
                    ) VALUES (
                        ?, ?, 'DOMAIN', 'PAYMENT_ACCEPTED',
                        'SUCCESS', 'SERVICE', 'svc-audit',
                        'PAYMENT_ACCEPTED', 'PAYMENT', ?,
                        'SUCCESS', 'PAYMENT_ACCEPTED',
                        ?, 'SIXPAY', 1,
                        'WORM_REFERENCE', 'proof-1',
                        ?
                    )
                    """,
                    evidenceId,
                    paymentId,
                    paymentId.toString(),
                    UUID.randomUUID(),
                    Timestamp.from(
                            Instant.parse(
                                    "2026-08-07T20:00:00Z"
                            )
                    )
            );

            PaymentAuditProjectionReadAdapter adapter =
                    new PaymentAuditProjectionReadAdapter(
                            jdbc
                    );

            var slice = adapter.search(
                    new AuditSearchCriteria(
                            paymentId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Instant.parse(
                                    "2026-08-07T00:00:00Z"
                            ),
                            Instant.parse(
                                    "2026-08-07T23:59:59Z"
                            ),
                            AuditSort.OCCURRED_AT_DESC,
                            50,
                            Instant.parse(
                                    "2026-08-07T21:00:00Z"
                            ),
                            null
                    )
            );

            assertEquals(1, slice.items().size());
            assertEquals(
                    evidenceId,
                    slice.items().getFirst().auditId()
            );
        }
    }
}

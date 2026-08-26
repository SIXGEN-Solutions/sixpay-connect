package com.sixpay.reporting.infrastructure.persistence;

import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.output.PaymentAuditAccessRecorder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcPaymentAuditAccessRecorder
        implements PaymentAuditAccessRecorder {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPaymentAuditAccessRecorder(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc is required");
    }

    @Override
    public void recordSuccessfulRead(
            String action,
            String targetType,
            String targetId,
            UUID correlationId,
            String actorId
    ) {
        UUID auditId = UUID.randomUUID();

        try {
            jdbc.update(
                    """
                    INSERT INTO reporting_payment_audit_evidence (
                        evidence_id,
                        timeline_visible,
                        audit_visible,
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
                        :auditId,
                        FALSE,
                        TRUE,
                        'DOMAIN',
                        'AUDIT_QUERY_ACCESSED',
                        'SUCCESS',
                        'SERVICE',
                        :actorId,
                        :action,
                        'AUDIT_QUERY',
                        :targetId,
                        'SUCCESS',
                        'AUDIT_QUERY_ALLOWED',
                        :correlationId,
                        'SIXPAY',
                        0,
                        'WORM_REFERENCE',
                        :integrityValue,
                        CURRENT_TIMESTAMP
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("auditId", auditId)
                            .addValue("actorId", actorId)
                            .addValue("action", action)
                            .addValue("targetId", targetId)
                            .addValue(
                                    "correlationId",
                                    correlationId
                            )
                            .addValue(
                                    "integrityValue",
                                    "reporting:" + auditId
                            )
            );
        } catch (DataAccessException exception) {
            throw new PaymentAuditQueryUnavailableException(
                    "Audit access trail is unavailable",
                    exception
            );
        }
    }
}

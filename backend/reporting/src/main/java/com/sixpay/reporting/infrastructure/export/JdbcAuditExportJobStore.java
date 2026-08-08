package com.sixpay.reporting.infrastructure.export;

import com.sixpay.reporting.application.exception.AuditExportConflictException;
import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.output.AuditExportJobStore;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JdbcAuditExportJobStore
        implements AuditExportJobStore {

    private static final String SEP = "\u001f";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAuditExportJobStore(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public AuditExportAcceptance accept(
            RequestPaymentAuditExportCommand command,
            String fingerprint,
            Instant requestedAt,
            Instant expiresAt
    ) {
        UUID exportId = UUID.randomUUID();

        try {
            int inserted = jdbc.update(
                    """
                    INSERT INTO reporting_payment_audit_export_job (
                        export_id,
                        idempotency_key,
                        request_fingerprint,
                        status,
                        occurred_from,
                        occurred_to,
                        payment_ids,
                        financial_institution_codes,
                        actions,
                        results,
                        business_purpose,
                        export_format,
                        requested_by,
                        correlation_id,
                        requested_at,
                        expires_at
                    ) VALUES (
                        :exportId,
                        :idempotencyKey,
                        :fingerprint,
                        'ACCEPTED',
                        :occurredFrom,
                        :occurredTo,
                        :paymentIds,
                        :institutions,
                        :actions,
                        :results,
                        :purpose,
                        :format,
                        :requestedBy,
                        :correlationId,
                        :requestedAt,
                        :expiresAt
                    )
                    ON CONFLICT (idempotency_key) DO NOTHING
                    """,
                    new MapSqlParameterSource()
                            .addValue("exportId", exportId)
                            .addValue(
                                    "idempotencyKey",
                                    command.idempotencyKey()
                            )
                            .addValue("fingerprint", fingerprint)
                            .addValue(
                                    "occurredFrom",
                                    command.occurredFrom()
                            )
                            .addValue(
                                    "occurredTo",
                                    command.occurredTo()
                            )
                            .addValue(
                                    "paymentIds",
                                    join(command.paymentIds())
                            )
                            .addValue(
                                    "institutions",
                                    join(
                                            command
                                                    .financialInstitutionCodes()
                                    )
                            )
                            .addValue(
                                    "actions",
                                    join(command.actions())
                            )
                            .addValue(
                                    "results",
                                    join(
                                            command.results()
                                                    .stream()
                                                    .map(Enum::name)
                                                    .toList()
                                    )
                            )
                            .addValue(
                                    "purpose",
                                    command.businessPurpose()
                            )
                            .addValue(
                                    "format",
                                    command.format().name()
                            )
                            .addValue(
                                    "requestedBy",
                                    command.requestedBy()
                            )
                            .addValue(
                                    "correlationId",
                                    command.correlationId()
                            )
                            .addValue(
                                    "requestedAt",
                                    requestedAt
                            )
                            .addValue("expiresAt", expiresAt)
            );

            AuditExportJobDefinition job =
                    findByIdempotencyKey(
                            command.idempotencyKey()
                    ).orElseThrow();

            if (!fingerprint.equals(
                    job.requestFingerprint()
            )) {
                throw new AuditExportConflictException(
                        "Idempotency-Key was reused "
                                + "with a different export request"
                );
            }

            return new AuditExportAcceptance(
                    job,
                    inserted == 1
            );
        } catch (AuditExportConflictException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public Optional<AuditExportJobDefinition> find(
            UUID exportId
    ) {
        return queryOne(
                """
                SELECT *
                FROM reporting_payment_audit_export_job
                WHERE export_id = :exportId
                """,
                new MapSqlParameterSource(
                        "exportId", exportId
                )
        );
    }

    @Override
    public Optional<AuditExportJobDefinition> claim(
            UUID exportId
    ) {
        try {
            int updated = jdbc.update(
                    """
                    UPDATE reporting_payment_audit_export_job
                    SET status = 'GENERATING',
                        generation_started_at = CURRENT_TIMESTAMP
                    WHERE export_id = :exportId
                      AND status = 'ACCEPTED'
                    """,
                    new MapSqlParameterSource(
                            "exportId", exportId
                    )
            );

            return updated == 1
                    ? find(exportId)
                    : Optional.empty();
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public List<UUID> findAccepted(int limit) {
        try {
            return jdbc.query(
                    """
                    SELECT export_id
                    FROM reporting_payment_audit_export_job
                    WHERE status = 'ACCEPTED'
                    ORDER BY requested_at ASC
                    LIMIT :limit
                    """,
                    new MapSqlParameterSource("limit", limit),
                    (rs, row) ->
                            rs.getObject(
                                    "export_id",
                                    UUID.class
                            )
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void complete(
            UUID exportId,
            long recordCount,
            String checksum,
            URI retrievalUri
    ) {
        updateStatus(
                """
                UPDATE reporting_payment_audit_export_job
                SET status = 'AVAILABLE',
                    record_count = :recordCount,
                    checksum = :checksum,
                    retrieval_uri = :retrievalUri,
                    completed_at = CURRENT_TIMESTAMP,
                    failure_code = NULL
                WHERE export_id = :exportId
                  AND status = 'GENERATING'
                """,
                new MapSqlParameterSource()
                        .addValue("exportId", exportId)
                        .addValue("recordCount", recordCount)
                        .addValue("checksum", checksum)
                        .addValue(
                                "retrievalUri",
                                retrievalUri.toString()
                        )
        );
    }

    @Override
    public void fail(
            UUID exportId,
            String failureCode
    ) {
        updateStatus(
                """
                UPDATE reporting_payment_audit_export_job
                SET status = 'FAILED',
                    failure_code = :failureCode,
                    completed_at = CURRENT_TIMESTAMP
                WHERE export_id = :exportId
                  AND status = 'GENERATING'
                """,
                new MapSqlParameterSource()
                        .addValue("exportId", exportId)
                        .addValue(
                                "failureCode",
                                failureCode
                        )
        );
    }

    @Override
    public void expire(Instant now) {
        try {
            jdbc.update(
                    """
                    UPDATE reporting_payment_audit_export_job
                    SET status = 'EXPIRED',
                        retrieval_uri = NULL
                    WHERE status = 'AVAILABLE'
                      AND expires_at <= :now
                    """,
                    new MapSqlParameterSource("now", now)
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<AuditExportJobDefinition>
            findByIdempotencyKey(String key) {
        return queryOne(
                """
                SELECT *
                FROM reporting_payment_audit_export_job
                WHERE idempotency_key = :key
                """,
                new MapSqlParameterSource("key", key)
        );
    }

    private Optional<AuditExportJobDefinition> queryOne(
            String sql,
            MapSqlParameterSource parameters
    ) {
        try {
            return jdbc.query(
                    sql,
                    parameters,
                    this::extractOne
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<AuditExportJobDefinition> extractOne(
            ResultSet rs
    ) throws SQLException {
        if (!rs.next()) {
            return Optional.empty();
        }
        return Optional.of(map(rs));
    }

    private AuditExportJobDefinition map(
            ResultSet rs
    ) throws SQLException {
        String uri = rs.getString("retrieval_uri");

        return new AuditExportJobDefinition(
                rs.getObject("export_id", UUID.class),
                rs.getString("idempotency_key"),
                rs.getString("request_fingerprint"),
                AuditExportStatus.valueOf(
                        rs.getString("status")
                ),
                rs.getObject(
                        "occurred_from", Instant.class
                ),
                rs.getObject(
                        "occurred_to", Instant.class
                ),
                splitUuid(rs.getString("payment_ids")),
                split(rs.getString(
                        "financial_institution_codes"
                )),
                split(rs.getString("actions")),
                split(rs.getString("results"))
                        .stream()
                        .map(AuditResult::valueOf)
                        .toList(),
                rs.getString("business_purpose"),
                AuditExportFormat.valueOf(
                        rs.getString("export_format")
                ),
                rs.getString("requested_by"),
                rs.getObject(
                        "correlation_id", UUID.class
                ),
                rs.getObject(
                        "requested_at", Instant.class
                ),
                rs.getObject("expires_at", Instant.class),
                nullableLong(rs, "record_count"),
                rs.getString("checksum"),
                uri == null ? null : URI.create(uri),
                rs.getString("failure_code")
        );
    }

    private void updateStatus(
            String sql,
            MapSqlParameterSource parameters
    ) {
        try {
            jdbc.update(sql, parameters);
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private static Long nullableLong(
            ResultSet rs,
            String column
    ) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static String join(List<?> values) {
        return values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(SEP));
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.asList(value.split(SEP, -1));
    }

    private static List<UUID> splitUuid(String value) {
        return split(value).stream()
                .map(UUID::fromString)
                .toList();
    }

    private static PaymentAuditQueryUnavailableException
            unavailable(DataAccessException exception) {
        return new PaymentAuditQueryUnavailableException(
                "Payment audit export store is unavailable",
                exception
        );
    }
}

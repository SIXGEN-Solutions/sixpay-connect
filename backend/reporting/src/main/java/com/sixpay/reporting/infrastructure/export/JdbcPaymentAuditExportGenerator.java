package com.sixpay.reporting.infrastructure.export;

import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.output.AuditExportGeneratorPort;
import com.sixpay.reporting.application.query.AuditExportJobDefinition;
import com.sixpay.reporting.application.query.GeneratedAuditExport;
import com.sixpay.reporting.domain.model.AuditExportFormat;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Component
public final class JdbcPaymentAuditExportGenerator
        implements AuditExportGeneratorPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPaymentAuditExportGenerator(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public GeneratedAuditExport generate(
            AuditExportJobDefinition job
    ) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(
                    "sixpay-audit-" + job.exportId() + "-",
                    job.format() == AuditExportFormat.CSV
                            ? ".csv"
                            : ".jsonl"
            );

            AtomicLong count = new AtomicLong();

            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                if (job.format() == AuditExportFormat.CSV) {
                    writer.write(
                            "auditId,occurredAt,actorType,actorId,"
                                    + "action,targetType,targetId,paymentId,"
                                    + "paymentReference,observedCustomerId,"
                                    + "result,reasonCode,correlationId,"
                                    + "sourceSystem,beforeState,afterState,"
                                    + "integrityScheme,integrityValue"
                    );
                    writer.newLine();
                }

                Query query = query(job);

                jdbc.query(
                        query.sql(),
                        query.parameters(),
                        rs -> {
                            writeRow(
                                    writer,
                                    rs,
                                    job.format()
                            );
                            count.incrementAndGet();
                        }
                );
            }

            return new GeneratedAuditExport(
                    temporary,
                    count.get(),
                    sha256(temporary)
            );
        } catch (DataAccessException exception) {
            deleteQuietly(temporary);
            throw new PaymentAuditQueryUnavailableException(
                    "Audit export source is unavailable",
                    exception
            );
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw new IllegalStateException(
                    "Cannot generate audit export",
                    exception
            );
        }
    }

    private static Query query(
            AuditExportJobDefinition job
    ) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT *
                FROM reporting_payment_audit_evidence
                WHERE audit_visible = TRUE
                  AND occurred_at >= :occurredFrom
                  AND occurred_at <= :occurredTo
                """
        );
        MapSqlParameterSource p =
                new MapSqlParameterSource()
                        .addValue(
                                "occurredFrom",
                                job.occurredFrom()
                        )
                        .addValue(
                                "occurredTo",
                                job.occurredTo()
                        );

        if (!job.paymentIds().isEmpty()) {
            sql.append(" AND payment_id IN (:paymentIds)");
            p.addValue("paymentIds", job.paymentIds());
        }
        if (!job.financialInstitutionCodes().isEmpty()) {
            sql.append(
                    " AND financial_institution_code "
                            + "IN (:institutionCodes)"
            );
            p.addValue(
                    "institutionCodes",
                    job.financialInstitutionCodes()
            );
        }
        if (!job.actions().isEmpty()) {
            sql.append(" AND action IN (:actions)");
            p.addValue("actions", job.actions());
        }
        if (!job.results().isEmpty()) {
            sql.append(" AND audit_result IN (:results)");
            p.addValue(
                    "results",
                    job.results().stream()
                            .map(Enum::name)
                            .toList()
            );
        }

        sql.append(
                " ORDER BY occurred_at ASC, evidence_id ASC"
        );

        return new Query(sql.toString(), p);
    }

    private static void writeRow(
            BufferedWriter writer,
            ResultSet rs,
            AuditExportFormat format
    ) {
        try {
            if (format == AuditExportFormat.CSV) {
                writer.write(csv(rs));
            } else {
                writer.write(jsonl(rs));
            }
            writer.newLine();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot serialize audit export row",
                    exception
            );
        }
    }

    private static String csv(ResultSet rs)
            throws Exception {
        return String.join(",",
                csvValue(rs.getString("evidence_id")),
                csvValue(rs.getString("occurred_at")),
                csvValue(rs.getString("actor_type")),
                csvValue(rs.getString("actor_id")),
                csvValue(rs.getString("action")),
                csvValue(rs.getString("target_type")),
                csvValue(rs.getString("target_id")),
                csvValue(rs.getString("payment_id")),
                csvValue(rs.getString("payment_reference")),
                csvValue(rs.getString("observed_customer_id")),
                csvValue(rs.getString("audit_result")),
                csvValue(rs.getString("reason_code")),
                csvValue(rs.getString("correlation_id")),
                csvValue(rs.getString("source_system")),
                csvValue(rs.getString("before_state")),
                csvValue(rs.getString("after_state")),
                csvValue(rs.getString("integrity_scheme")),
                csvValue(rs.getString("integrity_value"))
        );
    }

    private static String jsonl(ResultSet rs)
            throws Exception {
        return "{"
                + field("auditId", rs.getString("evidence_id")) + ","
                + field("occurredAt", rs.getString("occurred_at")) + ","
                + field("actorType", rs.getString("actor_type")) + ","
                + field("actorId", rs.getString("actor_id")) + ","
                + field("action", rs.getString("action")) + ","
                + field("targetType", rs.getString("target_type")) + ","
                + field("targetId", rs.getString("target_id")) + ","
                + field("paymentId", rs.getString("payment_id")) + ","
                + field("paymentReference", rs.getString("payment_reference")) + ","
                + field("observedCustomerId", rs.getString("observed_customer_id")) + ","
                + field("result", rs.getString("audit_result")) + ","
                + field("reasonCode", rs.getString("reason_code")) + ","
                + field("correlationId", rs.getString("correlation_id")) + ","
                + field("sourceSystem", rs.getString("source_system")) + ","
                + field("beforeState", rs.getString("before_state")) + ","
                + field("afterState", rs.getString("after_state")) + ","
                + field("integrityScheme", rs.getString("integrity_scheme")) + ","
                + field("integrityValue", rs.getString("integrity_value"))
                + "}";
    }

    private static String field(
            String name,
            String value
    ) {
        return "\"" + escape(name) + "\":"
                + (value == null
                ? "null"
                : "\"" + escape(value) + "\"");
    }

    private static String csvValue(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String sha256(Path file)
            throws IOException {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of()
                    .formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IOException(
                    "Cannot checksum audit export",
                    exception
            );
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record Query(
            String sql,
            MapSqlParameterSource parameters
    ) {
    }
}

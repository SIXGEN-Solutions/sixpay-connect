package com.sixpay.reporting.infrastructure.query;

import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.output.PaymentAuditReadPort;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PaymentAuditProjectionReadAdapter
        implements PaymentAuditReadPort {

    private final NamedParameterJdbcTemplate jdbc;

    public PaymentAuditProjectionReadAdapter(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc is required");
    }

    @Override
    public boolean paymentExists(UUID paymentId) {
        try {
            Integer count = jdbc.queryForObject(
                    """
                    SELECT COUNT(1)
                    FROM reporting_payment_audit_evidence
                    WHERE payment_id = :paymentId
                    """,
                    new MapSqlParameterSource("paymentId", paymentId),
                    Integer.class
            );
            return count != null && count > 0;
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public TimelineSlice timeline(TimelineCriteria criteria) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT *
                FROM reporting_payment_audit_evidence
                WHERE payment_id = :paymentId
                  AND timeline_visible = TRUE
                  AND occurred_at <= :snapshotAt
                """
        );
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("paymentId", criteria.paymentId())
                .addValue(
                        "snapshotAt",
                        timestamp(criteria.snapshotAt())
                );

        if (criteria.category() != null) {
            sql.append(" AND category = :category");
            p.addValue("category", criteria.category().name());
        }
        range(
                sql, p,
                criteria.occurredFrom(),
                criteria.occurredTo()
        );
        if (criteria.position() != null) {
            sql.append("""
                     AND (
                         occurred_at < :lastOccurredAt
                         OR (
                             occurred_at = :lastOccurredAt
                             AND evidence_id < :lastId
                         )
                     )
                    """);
            p.addValue(
                    "lastOccurredAt",
                    timestamp(criteria.position().occurredAt())
            );
            p.addValue("lastId", criteria.position().id());
        }

        sql.append(
                " ORDER BY occurred_at DESC, evidence_id DESC"
        );

        List<PaymentTimelineEntryView> rows =
                queryTimeline(sql.toString(), p, criteria.size());

        boolean more = rows.size() > criteria.size();
        List<PaymentTimelineEntryView> selected =
                more ? rows.subList(0, criteria.size()) : rows;

        AuditPosition next = more
                ? new AuditPosition(
                        selected.getLast().occurredAt(),
                        selected.getLast().timelineEntryId()
                )
                : null;

        return new TimelineSlice(selected, more, next);
    }

    @Override
    public AuditSlice search(AuditSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT *
                FROM reporting_payment_audit_evidence
                WHERE audit_visible = TRUE
                  AND occurred_at <= :snapshotAt
                  AND occurred_at >= :occurredFrom
                  AND occurred_at <= :occurredTo
                """
        );
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue(
                        "snapshotAt",
                        timestamp(criteria.snapshotAt())
                )
                .addValue(
                        "occurredFrom",
                        timestamp(criteria.occurredFrom())
                )
                .addValue(
                        "occurredTo",
                        timestamp(criteria.occurredTo())
                );

        eq(sql, p, "payment_id", "paymentId", criteria.paymentId());
        eq(sql, p, "payment_reference", "paymentReference",
                criteria.paymentReference());
        eq(sql, p, "observed_customer_id", "observedCustomerId",
                criteria.observedCustomerId());
        eq(sql, p, "actor_id", "actorId", criteria.actorId());
        eq(sql, p, "actor_type", "actorType",
                name(criteria.actorType()));
        eq(sql, p, "action", "action", criteria.action());
        eq(sql, p, "audit_result", "auditResult",
                name(criteria.result()));
        eq(sql, p, "reason_code", "reasonCode",
                criteria.reasonCode());
        eq(sql, p, "correlation_id", "correlationId",
                criteria.correlationId());
        eq(sql, p, "source_system", "sourceSystem",
                name(criteria.sourceSystem()));

        String comparison =
                criteria.sort() == AuditSort.OCCURRED_AT_ASC
                        ? ">"
                        : "<";
        String direction =
                criteria.sort() == AuditSort.OCCURRED_AT_ASC
                        ? "ASC"
                        : "DESC";

        if (criteria.position() != null) {
            sql.append(" AND (occurred_at ")
                    .append(comparison)
                    .append(" :lastOccurredAt OR (occurred_at = ")
                    .append(":lastOccurredAt AND evidence_id ")
                    .append(comparison)
                    .append(" :lastId))");
            p.addValue(
                    "lastOccurredAt",
                    timestamp(criteria.position().occurredAt())
            );
            p.addValue("lastId", criteria.position().id());
        }

        sql.append(" ORDER BY occurred_at ")
                .append(direction)
                .append(", evidence_id ")
                .append(direction);

        List<PaymentAuditRecordView> rows =
                queryAudit(sql.toString(), p, criteria.size());

        boolean more = rows.size() > criteria.size();
        List<PaymentAuditRecordView> selected =
                more ? rows.subList(0, criteria.size()) : rows;

        AuditPosition next = more
                ? new AuditPosition(
                        selected.getLast().occurredAt(),
                        selected.getLast().auditId()
                )
                : null;

        return new AuditSlice(selected, more, next);
    }

    @Override
    public Optional<PaymentAuditRecordView> findById(UUID auditId) {
        try {
            List<PaymentAuditRecordView> rows = jdbc.query(
                    """
                    SELECT *
                    FROM reporting_payment_audit_evidence
                    WHERE evidence_id = :auditId
                      AND audit_visible = TRUE
                    """,
                    new MapSqlParameterSource("auditId", auditId),
                    this::auditRow
            );
            return rows.stream().findFirst();
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private List<PaymentTimelineEntryView> queryTimeline(
            String sql,
            MapSqlParameterSource parameters,
            int size
    ) {
        try {
            return jdbc.query(
                    sql + " LIMIT " + (size + 1),
                    parameters,
                    this::timelineRow
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private List<PaymentAuditRecordView> queryAudit(
            String sql,
            MapSqlParameterSource parameters,
            int size
    ) {
        try {
            return jdbc.query(
                    sql + " LIMIT " + (size + 1),
                    parameters,
                    this::auditRow
            );
        } catch (DataAccessException exception) {
            throw unavailable(exception);
        }
    }

    private PaymentTimelineEntryView timelineRow(
            ResultSet rs,
            int row
    ) throws SQLException {
        return new PaymentTimelineEntryView(
                rs.getObject("evidence_id", UUID.class),
                rs.getObject("payment_id", UUID.class),
                AuditEvidenceCategory.valueOf(
                        rs.getString("category")
                ),
                rs.getString("event_type"),
                rs.getString("before_state"),
                rs.getString("after_state"),
                TimelineResult.valueOf(
                        rs.getString("timeline_result")
                ),
                rs.getString("reason_code"),
                instant(rs, "occurred_at"),
                rs.getObject("correlation_id", UUID.class),
                AuditSourceSystem.valueOf(
                        rs.getString("source_system")
                ),
                rs.getString("external_reference"),
                rs.getLong("aggregate_version"),
                Map.of()
        );
    }

    private PaymentAuditRecordView auditRow(
            ResultSet rs,
            int row
    ) throws SQLException {
        String roles = rs.getString("actor_roles");
        List<String> roleList = roles == null || roles.isBlank()
                ? List.of()
                : Arrays.stream(roles.split(","))
                        .map(String::strip)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toUnmodifiableList());

        return new PaymentAuditRecordView(
                rs.getObject("evidence_id", UUID.class),
                instant(rs, "occurred_at"),
                new AuditActorView(
                        AuditActorType.valueOf(
                                rs.getString("actor_type")
                        ),
                        rs.getString("actor_id"),
                        roleList
                ),
                rs.getString("action"),
                AuditTargetType.valueOf(
                        rs.getString("target_type")
                ),
                rs.getString("target_id"),
                rs.getObject("payment_id", UUID.class),
                rs.getString("payment_reference"),
                rs.getObject(
                        "observed_customer_id",
                        UUID.class
                ),
                AuditResult.valueOf(
                        rs.getString("audit_result")
                ),
                rs.getString("reason_code"),
                rs.getObject("correlation_id", UUID.class),
                rs.getString("trace_id"),
                AuditSourceSystem.valueOf(
                        rs.getString("source_system")
                ),
                rs.getString("before_state"),
                rs.getString("after_state"),
                Map.of(),
                new IntegrityEvidenceView(
                        AuditIntegrityScheme.valueOf(
                                rs.getString("integrity_scheme")
                        ),
                        rs.getString("integrity_value")
                )
        );
    }

    private static void range(
            StringBuilder sql,
            MapSqlParameterSource p,
            Instant from,
            Instant to
    ) {
        if (from != null) {
            sql.append(" AND occurred_at >= :occurredFrom");
            p.addValue(
                    "occurredFrom",
                    timestamp(from)
            );
        }
        if (to != null) {
            sql.append(" AND occurred_at <= :occurredTo");
            p.addValue(
                    "occurredTo",
                    timestamp(to)
            );
        }
    }

    private static void eq(
            StringBuilder sql,
            MapSqlParameterSource p,
            String column,
            String name,
            Object value
    ) {
        if (value != null) {
            sql.append(" AND ")
                    .append(column)
                    .append(" = :")
                    .append(name);
            p.addValue(name, value);
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(
            ResultSet rs,
            String column
    ) throws SQLException {
        OffsetDateTime value =
                rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static PaymentAuditQueryUnavailableException unavailable(
            DataAccessException exception
    ) {
        return new PaymentAuditQueryUnavailableException(
                "Payment audit projection is unavailable",
                exception
        );
    }
}

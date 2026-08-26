package com.sixpay.customer.observation.infrastructure.query.adapter;

import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerPaymentQueryRepository;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentSlice;
import com.sixpay.customer.observation.infrastructure.query.mapper
        .ObservedCustomerQueryRowMapper;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedPaymentRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Native JPA keyset adapter for linked observed Payments.
 */
public final class JpaObservedCustomerPaymentQueryAdapter
        implements ObservedCustomerPaymentQueryRepository {

    private final EntityManager entityManager;
    private final ObservedCustomerQueryRowMapper mapper;

    public JpaObservedCustomerPaymentQueryAdapter(
            EntityManager entityManager,
            ObservedCustomerQueryRowMapper mapper
    ) {
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "entityManager is required"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
    }

    @Override
    public ObservedCustomerPaymentSlice findByCustomerId(
            ObservedCustomerPaymentCriteria criteria
    ) {
        Objects.requireNonNull(
                criteria,
                "criteria is required"
        );

        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    p.payment_id,
                    p.public_payment_reference,
                    p.financial_institution_code,
                    p.amount,
                    p.currency,
                    p.payment_status,
                    p.failure_reason_code,
                    p.payment_created_at,
                    p.payment_updated_at
                FROM customer_observed_payment p
                WHERE p.observed_customer_id = :customerId
                  AND p.payment_updated_at <= :snapshotAt
                """
        );

        Map<String, Object> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "customerId",
                criteria.observedCustomerId().value()
        );
        parameters.put(
                "snapshotAt",
                criteria.snapshotAt()
        );

        if (criteria.status() != null) {
            sql.append(
                    " AND p.payment_status = :paymentStatus "
            );
            parameters.put(
                    "paymentStatus",
                    criteria.status().name()
            );
        }

        if (criteria.createdFrom() != null) {
            sql.append(
                    " AND p.payment_created_at >= :createdFrom "
            );
            parameters.put(
                    "createdFrom",
                    criteria.createdFrom()
            );
        }

        if (criteria.createdTo() != null) {
            sql.append(
                    " AND p.payment_created_at <= :createdTo "
            );
            parameters.put(
                    "createdTo",
                    criteria.createdTo()
            );
        }

        ObservedCustomerPaymentPosition position =
                criteria.position();

        if (position != null) {
            sql.append(
                    """
                    AND (
                        p.payment_created_at < :lastCreatedAt
                        OR (
                            p.payment_created_at = :lastCreatedAt
                            AND p.payment_id < :lastPaymentId
                        )
                    )
                    """
            );

            parameters.put(
                    "lastCreatedAt",
                    position.lastPaymentCreatedAt()
            );
            parameters.put(
                    "lastPaymentId",
                    position.lastPaymentId()
            );
        }

        sql.append(
                """
                ORDER BY
                    p.payment_created_at DESC,
                    p.payment_id DESC
                """
        );

        Query query = entityManager.createNativeQuery(
                sql.toString()
        );

        parameters.forEach(
                query::setParameter
        );

        query.setMaxResults(
                Math.addExact(criteria.size(), 1)
        );

        List<Object[]> rawRows = rows(query);

        boolean hasMore =
                rawRows.size() > criteria.size();

        List<Object[]> selected = hasMore
                ? rawRows.subList(0, criteria.size())
                : rawRows;

        List<ObservedPaymentRow> paymentRows =
                selected.stream()
                        .map(this::paymentRow)
                        .toList();

        ObservedCustomerPaymentPosition nextPosition =
                hasMore
                        ? positionOf(
                                paymentRows.getLast()
                        )
                        : null;

        return new ObservedCustomerPaymentSlice(
                paymentRows.stream()
                        .map(mapper::toPayment)
                        .toList(),
                hasMore,
                nextPosition
        );
    }

    private ObservedPaymentRow paymentRow(
            Object[] row
    ) {
        return new ObservedPaymentRow(
                uuid(row[0]),
                text(row[1]),
                text(row[2]),
                decimal(row[3]),
                text(row[4]),
                text(row[5]),
                nullableText(row[6]),
                instant(row[7]),
                instant(row[8])
        );
    }

    private static ObservedCustomerPaymentPosition positionOf(
            ObservedPaymentRow row
    ) {
        return new ObservedCustomerPaymentPosition(
                row.paymentCreatedAt(),
                row.paymentId()
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(
            Query query
    ) {
        return (List<Object[]>) query.getResultList();
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }

        return UUID.fromString(
                Objects.toString(value)
        );
    }

    private static String text(Object value) {
        return Objects.requireNonNull(
                value,
                "query column is required"
        ).toString();
    }

    private static String nullableText(Object value) {
        return value == null ? null : value.toString();
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        return new BigDecimal(
                Objects.toString(value)
        );
    }

    private static Instant instant(Object value) {
        Objects.requireNonNull(
                value,
                "timestamp query column is required"
        );

        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof OffsetDateTime offset) {
            return offset.toInstant();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }

        throw new IllegalStateException(
                "Unsupported timestamp value: "
                        + value.getClass().getName()
        );
    }
}

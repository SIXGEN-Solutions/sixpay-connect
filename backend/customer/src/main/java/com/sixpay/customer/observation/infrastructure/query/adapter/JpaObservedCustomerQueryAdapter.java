package com.sixpay.customer.observation.infrastructure.query.adapter;

import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerQueryRepository;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchSlice;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSort;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.infrastructure.persistence.protection
        .ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.query.mapper
        .ObservedCustomerQueryRowMapper;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerDetailRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerSummaryRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedInstitutionRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Native JPA read adapter implementing stable keyset pagination.
 */
public final class JpaObservedCustomerQueryAdapter
        implements ObservedCustomerQueryRepository {

    private static final int MINIMUM_LEGAL_NAME_PREFIX = 2;

    private static final String SUMMARY_COLUMNS = """
            c.observed_customer_id,
            c.niu_protected,
            c.legal_name_protected,
            c.phone_masked,
            c.email_masked,
            c.first_observed_at,
            c.last_observed_at,
            c.total_payments,
            c.successful_payments,
            c.failed_payments,
            c.last_payment_status,
            c.last_failure_reason_code,
            c.updated_at,
            c.projection_version
            """;

    private final EntityManager entityManager;
    private final ObservedCustomerDataProtector protector;
    private final ObservedCustomerQueryRowMapper mapper;

    public JpaObservedCustomerQueryAdapter(
            EntityManager entityManager,
            ObservedCustomerDataProtector protector,
            ObservedCustomerQueryRowMapper mapper
    ) {
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "entityManager is required"
        );
        this.protector = Objects.requireNonNull(
                protector,
                "protector is required"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
    }

    @Override
    public ObservedCustomerSearchSlice search(
            ObservedCustomerSearchCriteria criteria
    ) {
        Objects.requireNonNull(
                criteria,
                "criteria is required"
        );

        StringBuilder sql = new StringBuilder(
                "SELECT " + SUMMARY_COLUMNS
                        + " FROM customer_observed_customer c "
                        + "WHERE c.updated_at <= :snapshotAt "
        );

        Map<String, Object> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "snapshotAt",
                criteria.snapshotAt()
        );

        appendFilters(
                sql,
                parameters,
                criteria
        );

        SortExpression sort =
                SortExpression.forSort(
                        criteria.sort()
                );

        appendKeyset(
                sql,
                parameters,
                criteria.position(),
                sort
        );

        sql.append(" ORDER BY ")
                .append(sort.column())
                .append(' ')
                .append(sort.direction())
                .append(", c.observed_customer_id ")
                .append(sort.direction());

        Query query = entityManager.createNativeQuery(
                sql.toString()
        );

        bind(query, parameters);

        query.setMaxResults(
                Math.addExact(criteria.size(), 1)
        );

        List<Object[]> rawRows = rows(query);

        boolean hasMore =
                rawRows.size() > criteria.size();

        List<Object[]> selected = hasMore
                ? rawRows.subList(0, criteria.size())
                : rawRows;

        List<ObservedCustomerSummaryRow> rows =
                selected.stream()
                        .map(this::summaryRow)
                        .toList();

        ObservedCustomerSearchPosition nextPosition =
                hasMore
                        ? positionOf(
                                rows.getLast(),
                                criteria.sort()
                        )
                        : null;

        return new ObservedCustomerSearchSlice(
                rows.stream()
                        .map(mapper::toSummary)
                        .toList(),
                hasMore,
                nextPosition
        );
    }

    @Override
    public Optional<ObservedCustomerDetailView> findDetailById(
            ObservedCustomerId observedCustomerId
    ) {
        Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );

        Query query = entityManager.createNativeQuery(
                "SELECT " + SUMMARY_COLUMNS + ", "
                        + "c.source_event_watermark "
                        + "FROM customer_observed_customer c "
                        + "WHERE c.observed_customer_id = :customerId"
        );

        query.setParameter(
                "customerId",
                observedCustomerId.value()
        );

        List<Object[]> customers = rows(query);

        if (customers.isEmpty()) {
            return Optional.empty();
        }

        Object[] customer = customers.getFirst();

        List<ObservedInstitutionRow> institutions =
                loadInstitutions(
                        observedCustomerId.value()
                );

        ObservedCustomerDetailRow detail =
                new ObservedCustomerDetailRow(
                        uuid(customer[0]),
                        text(customer[1]),
                        text(customer[2]),
                        nullableText(customer[3]),
                        nullableText(customer[4]),
                        institutions,
                        instant(customer[5]),
                        instant(customer[6]),
                        number(customer[7]),
                        number(customer[8]),
                        number(customer[9]),
                        text(customer[10]),
                        nullableText(customer[11]),
                        instant(customer[12]),
                        number(customer[13]),
                        text(customer[14])
                );

        return Optional.of(
                mapper.toDetail(detail)
        );
    }

    @Override
    public boolean existsById(
            ObservedCustomerId observedCustomerId
    ) {
        Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );

        Query query = entityManager.createNativeQuery(
                """
                SELECT 1
                FROM customer_observed_customer
                WHERE observed_customer_id = :customerId
                """
        );

        query.setParameter(
                "customerId",
                observedCustomerId.value()
        );
        query.setMaxResults(1);

        return !query.getResultList().isEmpty();
    }

    private void appendFilters(
            StringBuilder sql,
            Map<String, Object> parameters,
            ObservedCustomerSearchCriteria criteria
    ) {
        if (criteria.normalizedNiu() != null) {
            sql.append(
                    "AND c.niu_search_hash = :niuSearchHash "
            );
            parameters.put(
                    "niuSearchHash",
                    protector.searchHash(
                            normalizeNiu(
                                    criteria.normalizedNiu()
                            )
                    )
            );
        }

        if (criteria.legalName() != null) {
            String prefix = normalizeLegalName(
                    criteria.legalName()
            );

            if (prefix.length()
                    < MINIMUM_LEGAL_NAME_PREFIX) {
                throw new IllegalArgumentException(
                        "legalName must contain at least "
                                + MINIMUM_LEGAL_NAME_PREFIX
                                + " normalized characters"
                );
            }

            sql.append(
                    "AND c.legal_name_search_normalized "
                            + "LIKE :legalNamePrefix ESCAPE '\\' "
            );
            parameters.put(
                    "legalNamePrefix",
                    escapeLike(prefix) + "%"
            );
        }

        if (criteria.financialInstitutionCode() != null) {
            sql.append(
                    """
                    AND EXISTS (
                        SELECT 1
                        FROM customer_observed_institution i
                        WHERE i.observed_customer_id =
                              c.observed_customer_id
                          AND i.financial_institution_code =
                              :institutionCode
                    )
                    """
            );
            parameters.put(
                    "institutionCode",
                    criteria.financialInstitutionCode()
                            .strip()
                            .toUpperCase(Locale.ROOT)
            );
        }

        if (criteria.lastPaymentStatus() != null) {
            sql.append(
                    "AND c.last_payment_status = :lastStatus "
            );
            parameters.put(
                    "lastStatus",
                    criteria.lastPaymentStatus().name()
            );
        }

        if (criteria.lastFailureReasonCode() != null) {
            sql.append(
                    "AND c.last_failure_reason_code = :failureCode "
            );
            parameters.put(
                    "failureCode",
                    criteria.lastFailureReasonCode()
                            .strip()
                            .toUpperCase(Locale.ROOT)
            );
        }

        appendRange(
                sql,
                parameters,
                "c.first_observed_at",
                "firstObservedFrom",
                criteria.firstObservedFrom(),
                "firstObservedTo",
                criteria.firstObservedTo()
        );

        appendRange(
                sql,
                parameters,
                "c.last_observed_at",
                "lastObservedFrom",
                criteria.lastObservedFrom(),
                "lastObservedTo",
                criteria.lastObservedTo()
        );

        if (criteria.paymentFrom() != null
                || criteria.paymentTo() != null) {
            sql.append(
                    """
                    AND EXISTS (
                        SELECT 1
                        FROM customer_observed_payment p
                        WHERE p.observed_customer_id =
                              c.observed_customer_id
                    """
            );

            if (criteria.paymentFrom() != null) {
                sql.append(
                        " AND p.payment_created_at >= :paymentFrom "
                );
                parameters.put(
                        "paymentFrom",
                        criteria.paymentFrom()
                );
            }

            if (criteria.paymentTo() != null) {
                sql.append(
                        " AND p.payment_created_at <= :paymentTo "
                );
                parameters.put(
                        "paymentTo",
                        criteria.paymentTo()
                );
            }

            sql.append(") ");
        }
    }

    private static void appendKeyset(
            StringBuilder sql,
            Map<String, Object> parameters,
            ObservedCustomerSearchPosition position,
            SortExpression sort
    ) {
        if (position == null) {
            return;
        }

        String comparison =
                sort.ascending() ? ">" : "<";

        sql.append("AND (")
                .append(sort.column())
                .append(' ')
                .append(comparison)
                .append(" :lastSortValue OR (")
                .append(sort.column())
                .append(" = :lastSortValue ")
                .append("AND c.observed_customer_id ")
                .append(comparison)
                .append(" :lastCustomerId)) ");

        parameters.put(
                "lastSortValue",
                position.lastSortValue()
        );
        parameters.put(
                "lastCustomerId",
                position.lastObservedCustomerId()
                        .value()
        );
    }

    private List<ObservedInstitutionRow> loadInstitutions(
            UUID customerId
    ) {
        Query query = entityManager.createNativeQuery(
                """
                SELECT
                    i.observed_institution_id,
                    i.financial_institution_code,
                    i.first_observed_at,
                    i.last_observed_at,
                    a.observed_account_id,
                    a.masked_value
                FROM customer_observed_institution i
                LEFT JOIN customer_observed_account a
                  ON a.observed_institution_id =
                     i.observed_institution_id
                WHERE i.observed_customer_id = :customerId
                ORDER BY i.financial_institution_code ASC,
                         a.observed_account_id ASC
                """
        );

        query.setParameter(
                "customerId",
                customerId
        );

        Map<UUID, InstitutionAccumulator> grouped =
                new LinkedHashMap<>();

        for (Object[] row : rows(query)) {
            UUID institutionId = uuid(row[0]);

            InstitutionAccumulator accumulator =
                    grouped.computeIfAbsent(
                            institutionId,
                            ignored ->
                                    new InstitutionAccumulator(
                                            institutionId,
                                            text(row[1]),
                                            instant(row[2]),
                                            instant(row[3])
                                    )
                    );

            if (row[4] != null) {
                accumulator.accounts().add(
                        new ObservedInstitutionRow.AccountRow(
                                uuid(row[4]),
                                text(row[5])
                        )
                );
            }
        }

        return grouped.values()
                .stream()
                .map(InstitutionAccumulator::toRow)
                .toList();
    }

    private ObservedCustomerSummaryRow summaryRow(
            Object[] row
    ) {
        return new ObservedCustomerSummaryRow(
                uuid(row[0]),
                text(row[1]),
                text(row[2]),
                nullableText(row[3]),
                nullableText(row[4]),
                instant(row[5]),
                instant(row[6]),
                number(row[7]),
                number(row[8]),
                number(row[9]),
                text(row[10]),
                nullableText(row[11]),
                instant(row[12]),
                number(row[13])
        );
    }

    private static ObservedCustomerSearchPosition positionOf(
            ObservedCustomerSummaryRow row,
            ObservedCustomerSort sort
    ) {
        Instant value = switch (sort) {
            case FIRST_OBSERVED_AT_ASC,
                 FIRST_OBSERVED_AT_DESC ->
                    row.firstObservedAt();
            case LAST_OBSERVED_AT_ASC,
                 LAST_OBSERVED_AT_DESC ->
                    row.lastObservedAt();
        };

        return new ObservedCustomerSearchPosition(
                value,
                ObservedCustomerId.of(
                        row.observedCustomerId()
                )
        );
    }

    private static void appendRange(
            StringBuilder sql,
            Map<String, Object> parameters,
            String column,
            String fromName,
            Instant from,
            String toName,
            Instant to
    ) {
        if (from != null) {
            sql.append("AND ")
                    .append(column)
                    .append(" >= :")
                    .append(fromName)
                    .append(' ');
            parameters.put(fromName, from);
        }

        if (to != null) {
            sql.append("AND ")
                    .append(column)
                    .append(" <= :")
                    .append(toName)
                    .append(' ');
            parameters.put(toName, to);
        }
    }

    private static void bind(
            Query query,
            Map<String, Object> parameters
    ) {
        parameters.forEach(
                query::setParameter
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

    private static long number(Object value) {
        return ((Number) Objects.requireNonNull(
                value,
                "numeric query column is required"
        )).longValue();
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

    private static String normalizeNiu(String value) {
        return value.strip()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeLegalName(String value) {
        String decomposed = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .strip()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private record SortExpression(
            String column,
            String direction,
            boolean ascending
    ) {

        private static SortExpression forSort(
                ObservedCustomerSort sort
        ) {
            return switch (sort) {
                case FIRST_OBSERVED_AT_ASC ->
                        new SortExpression(
                                "c.first_observed_at",
                                "ASC",
                                true
                        );
                case FIRST_OBSERVED_AT_DESC ->
                        new SortExpression(
                                "c.first_observed_at",
                                "DESC",
                                false
                        );
                case LAST_OBSERVED_AT_ASC ->
                        new SortExpression(
                                "c.last_observed_at",
                                "ASC",
                                true
                        );
                case LAST_OBSERVED_AT_DESC ->
                        new SortExpression(
                                "c.last_observed_at",
                                "DESC",
                                false
                        );
            };
        }
    }

    private record InstitutionAccumulator(
            UUID id,
            String code,
            Instant firstObservedAt,
            Instant lastObservedAt,
            List<ObservedInstitutionRow.AccountRow> accounts
    ) {

        private InstitutionAccumulator(
                UUID id,
                String code,
                Instant firstObservedAt,
                Instant lastObservedAt
        ) {
            this(
                    id,
                    code,
                    firstObservedAt,
                    lastObservedAt,
                    new ArrayList<>()
            );
        }

        private ObservedInstitutionRow toRow() {
            return new ObservedInstitutionRow(
                    id,
                    code,
                    firstObservedAt,
                    lastObservedAt,
                    accounts
            );
        }
    }
}

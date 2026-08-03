package com.sixpay.payment.infrastructure.query;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.security.PaymentProjectionReadPort;
import com.sixpay.payment.application.query.PaymentSearchSort;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentVisibilityScope;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only Payment projection adapter.
 *
 * <p>It never loads or reconstitutes the Payment Aggregate Root. All filters,
 * cursor boundaries and visibility constraints are applied in PostgreSQL.</p>
 */
@Component
public final class PaymentProjectionReadAdapter
        implements PaymentProjectionReadPort {

    private static final String PROJECTION_COLUMNS = """
            p.payment_id,
            p.public_payment_reference,
            p.external_payment_reference,
            p.financial_institution_code,
            p.requested_amount,
            BTRIM(p.requested_currency) AS requested_currency,
            p.status,
            p.business_version,
            p.received_at,
            p.updated_at,
            p.finalized_at,
            p.state_payload #>>
                '{debtorAccountReference,bindingFingerprint}'
                AS debtor_account_reference,
            p.state_payload #>>
                '{debtorAccountReference,maskedDisplay}'
                AS debtor_account_masked,
            p.state_payload #>>
                '{failure,failureCode}'
                AS reason_code,
            COALESCE(
                p.state_payload #>>
                    '{requestIdentity,correlationId,value}',
                (
                    SELECT a.correlation_id
                      FROM payment_audit a
                     WHERE a.payment_id = p.payment_id
                     ORDER BY
                           a.business_version DESC,
                           a.event_sequence DESC
                     LIMIT 1
                )
            ) AS correlation_id,
            p.state_payload #>>
                '{bankingVerificationEvidence,verificationId,value}'
                AS banking_verification_id,
            p.state_payload #>>
                '{bankingVerificationEvidence,outcome}'
                AS banking_outcome,
            p.state_payload #>>
                '{bankingVerificationEvidence,metadata,observedAt}'
                AS banking_observed_at,
            ARRAY(
                SELECT DISTINCT check_value ->> 'reasonCode'
                  FROM jsonb_array_elements(
                       COALESCE(
                           p.state_payload #>
                               '{bankingVerificationEvidence,checks}',
                           '[]'::jsonb
                       )
                  ) AS check_value
                 WHERE check_value ->> 'reasonCode' IS NOT NULL
                 ORDER BY check_value ->> 'reasonCode'
            ) AS banking_reason_codes,
            p.state_payload #>>
                '{bankPostingReference,principalPostingReference}'
                AS bank_posting_reference,
            p.state_payload #>>
                '{postingOutcomeEvidence,outcome}'
                AS posting_outcome,
            p.state_payload #>>
                '{postingOutcomeEvidence,metadata,observedAt}'
                AS posting_observed_at,
            p.state_payload #>>
                '{endOfDayConfirmationEvidence,tfjStatus}'
                AS tfj_status,
            p.state_payload #>>
                '{endOfDayConfirmationEvidence,businessDate}'
                AS tfj_business_date,
            p.state_payload #>>
                '{endOfDayConfirmationEvidence,confirmedAt}'
                AS tfj_confirmed_at,
            p.state_payload #>>
                '{reversalEvidence,outcome,outcome}'
                AS reversal_outcome,
            p.state_payload #>>
                '{reversalEvidence,outcome,reversalReference,value}'
                AS reversal_reference,
            p.state_payload #>>
                '{reversalEvidence,outcome,metadata,observedAt}'
                AS reversal_observed_at
            """;

    private static final RowMapper<ProjectionRow> ROW_MAPPER =
            PaymentProjectionReadAdapter::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TimeProvider timeProvider;
    private final PaymentProjectionCursorCodec cursorCodec;

    public PaymentProjectionReadAdapter(
            NamedParameterJdbcTemplate jdbcTemplate,
            TimeProvider timeProvider,
            PaymentProjectionCursorCodec cursorCodec
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "Named-parameter JDBC template"
        );
        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
        this.cursorCodec = Objects.requireNonNull(
                cursorCodec,
                "Payment cursor codec"
        );
    }

    @Override
    public PaymentProjectionViews.SearchPage search(
            SearchPaymentProjectionsQuery query,
            PaymentVisibilityScope visibility
    ) {
        Objects.requireNonNull(query, "Payment search query");
        Objects.requireNonNull(
                visibility,
                "Payment visibility scope"
        );

        Instant defaultSnapshot = timeProvider.now();

        if (visibility instanceof PaymentVisibilityScope.Partner
                || query.observedCustomerId() != null) {
            return emptyPage(defaultSnapshot);
        }

        PaymentProjectionCursorCodec.Cursor cursor =
                query.cursor() == null
                        ? null
                        : cursorCodec.decode(
                                query.cursor(),
                                query.sort()
                        );

        Instant snapshotAt = cursor == null
                ? defaultSnapshot
                : cursor.snapshotAt();

        SortDefinition sort = SortDefinition.from(query.sort());

        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("snapshotAt", snapshotAt)
                        .addValue("limit", query.size() + 1);

        StringBuilder where = new StringBuilder(
                " WHERE "
                        + sort.column()
                        + " <= :snapshotAt"
        );

        appendEquals(
                where,
                parameters,
                "p.public_payment_reference",
                "paymentReference",
                query.paymentReference()
        );
        appendEquals(
                where,
                parameters,
                "p.external_payment_reference",
                "tresorPayRequestId",
                query.tresorPayRequestId()
        );
        appendEquals(
                where,
                parameters,
                "p.financial_institution_code",
                "financialInstitutionCode",
                query.financialInstitutionCode()
        );
        appendEquals(
                where,
                parameters,
                "p.status",
                "status",
                query.status()
        );
        appendEquals(
                where,
                parameters,
                "p.state_payload #>> '{failure,failureCode}'",
                "reasonCode",
                query.reasonCode()
        );
        appendEquals(
                where,
                parameters,
                "BTRIM(p.requested_currency)",
                "currency",
                normalizeCurrency(query.currency())
        );

        appendLowerBound(
                where,
                parameters,
                "p.received_at",
                "createdFrom",
                query.createdFrom()
        );
        appendUpperBound(
                where,
                parameters,
                "p.received_at",
                "createdTo",
                query.createdTo()
        );
        appendLowerBound(
                where,
                parameters,
                "p.requested_amount",
                "amountMin",
                query.amountMin()
        );
        appendUpperBound(
                where,
                parameters,
                "p.requested_amount",
                "amountMax",
                query.amountMax()
        );

        if (cursor != null) {
            parameters
                    .addValue("cursorAt", cursor.positionAt())
                    .addValue("cursorId", cursor.paymentId());

            where.append(" AND (")
                    .append(sort.column())
                    .append(sort.ascending() ? " > " : " < ")
                    .append(":cursorAt OR (")
                    .append(sort.column())
                    .append(" = :cursorAt AND p.payment_id ")
                    .append(sort.ascending() ? " > " : " < ")
                    .append(":cursorId))");
        }

        String sql = """
                SELECT
                """
                + PROJECTION_COLUMNS
                + """
                  FROM payments p
                """
                + where
                + " ORDER BY "
                + sort.column()
                + (sort.ascending() ? " ASC" : " DESC")
                + ", p.payment_id "
                + (sort.ascending() ? "ASC" : "DESC")
                + " LIMIT :limit";

        List<ProjectionRow> rows = jdbcTemplate.query(
                sql,
                parameters,
                ROW_MAPPER
        );

        boolean hasMore = rows.size() > query.size();
        List<ProjectionRow> selected = hasMore
                ? rows.subList(0, query.size())
                : rows;

        List<PaymentProjectionViews.Summary> items =
                selected.stream()
                        .map(ProjectionRow::toSummary)
                        .toList();

        String nextCursor = null;
        if (hasMore && !selected.isEmpty()) {
            ProjectionRow last =
                    selected.get(selected.size() - 1);

            nextCursor = cursorCodec.encode(
                    query.sort(),
                    snapshotAt,
                    sort.position(last),
                    last.paymentId()
            );
        }

        return new PaymentProjectionViews.SearchPage(
                items,
                items.size(),
                hasMore,
                nextCursor,
                snapshotAt
        );
    }

    @Override
    public Optional<PaymentProjectionViews.Detail> findById(
            PaymentId paymentId
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");

        String sql = """
                SELECT
                """
                + PROJECTION_COLUMNS
                + """
                  FROM payments p
                 WHERE p.payment_id = :paymentId
                """;

        List<ProjectionRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource(
                        "paymentId",
                        paymentId.value()
                ),
                ROW_MAPPER
        );

        return rows.stream()
                .findFirst()
                .map(ProjectionRow::toDetail);
    }

    private static PaymentProjectionViews.SearchPage emptyPage(
            Instant snapshotAt
    ) {
        return new PaymentProjectionViews.SearchPage(
                List.of(),
                0,
                false,
                null,
                snapshotAt
        );
    }

    private static void appendEquals(
            StringBuilder where,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            Object value
    ) {
        if (value == null) {
            return;
        }
        if (value instanceof String string
                && string.isBlank()) {
            return;
        }

        where.append(" AND ")
                .append(column)
                .append(" = :")
                .append(parameter);
        parameters.addValue(parameter, value);
    }

    private static void appendLowerBound(
            StringBuilder where,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            Object value
    ) {
        if (value == null) {
            return;
        }

        where.append(" AND ")
                .append(column)
                .append(" >= :")
                .append(parameter);
        parameters.addValue(parameter, value);
    }

    private static void appendUpperBound(
            StringBuilder where,
            MapSqlParameterSource parameters,
            String column,
            String parameter,
            Object value
    ) {
        if (value == null) {
            return;
        }

        where.append(" AND ")
                .append(column)
                .append(" <= :")
                .append(parameter);
        parameters.addValue(parameter, value);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null
                ? null
                : currency.strip().toUpperCase(Locale.ROOT);
    }

    private static ProjectionRow mapRow(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        return new ProjectionRow(
                resultSet.getObject("payment_id", UUID.class),
                resultSet.getString("public_payment_reference"),
                resultSet.getString("external_payment_reference"),
                resultSet.getString("financial_institution_code"),
                resultSet.getBigDecimal("requested_amount"),
                resultSet.getString("requested_currency"),
                resultSet.getString("status"),
                resultSet.getLong("business_version"),
                instant(resultSet, "received_at"),
                instant(resultSet, "updated_at"),
                instant(resultSet, "finalized_at"),
                resultSet.getString("debtor_account_reference"),
                resultSet.getString("debtor_account_masked"),
                resultSet.getString("reason_code"),
                resultSet.getString("correlation_id"),
                resultSet.getString("banking_verification_id"),
                resultSet.getString("banking_outcome"),
                textArray(resultSet, "banking_reason_codes"),
                instantText(
                        resultSet.getString(
                                "banking_observed_at"
                        )
                ),
                resultSet.getString("bank_posting_reference"),
                resultSet.getString("posting_outcome"),
                instantText(
                        resultSet.getString(
                                "posting_observed_at"
                        )
                ),
                resultSet.getString("tfj_status"),
                localDate(
                        resultSet.getString(
                                "tfj_business_date"
                        )
                ),
                instantText(
                        resultSet.getString(
                                "tfj_confirmed_at"
                        )
                ),
                resultSet.getString("reversal_outcome"),
                resultSet.getString("reversal_reference"),
                instantText(
                        resultSet.getString(
                                "reversal_observed_at"
                        )
                )
        );
    }

    private static Instant instant(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        OffsetDateTime value = resultSet.getObject(
                column,
                OffsetDateTime.class
        );
        return value == null ? null : value.toInstant();
    }

    private static Instant instantText(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static LocalDate localDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    private static List<String> textArray(
            ResultSet resultSet,
            String column
    ) throws SQLException {
        Array array = resultSet.getArray(column);
        if (array == null) {
            return List.of();
        }

        Object raw = array.getArray();
        if (raw instanceof String[] strings) {
            return List.copyOf(Arrays.asList(strings));
        }
        if (raw instanceof Object[] values) {
            List<String> strings = new ArrayList<>(values.length);
            for (Object value : values) {
                if (value != null) {
                    strings.add(value.toString());
                }
            }
            return List.copyOf(strings);
        }
        return List.of();
    }

    private record SortDefinition(
            String column,
            boolean ascending
    ) {
        private static SortDefinition from(
                PaymentSearchSort sort
        ) {
            return switch (sort) {
                case CREATED_AT_ASC ->
                        new SortDefinition(
                                "p.received_at",
                                true
                        );
                case CREATED_AT_DESC ->
                        new SortDefinition(
                                "p.received_at",
                                false
                        );
                case UPDATED_AT_ASC ->
                        new SortDefinition(
                                "p.updated_at",
                                true
                        );
                case UPDATED_AT_DESC ->
                        new SortDefinition(
                                "p.updated_at",
                                false
                        );
            };
        }

        private Instant position(ProjectionRow row) {
            return column.endsWith("received_at")
                    ? row.receivedAt()
                    : row.updatedAt();
        }
    }

    private record ProjectionRow(
            UUID paymentId,
            String paymentReference,
            String tresorPayRequestId,
            String financialInstitutionCode,
            java.math.BigDecimal amount,
            String currency,
            String status,
            long businessVersion,
            Instant receivedAt,
            Instant updatedAt,
            Instant finalizedAt,
            String debtorAccountReference,
            String debtorAccountMasked,
            String reasonCode,
            String correlationId,
            String bankingVerificationId,
            String bankingOutcome,
            List<String> bankingReasonCodes,
            Instant bankingObservedAt,
            String bankPostingReference,
            String postingOutcome,
            Instant postingObservedAt,
            String tfjStatus,
            LocalDate tfjBusinessDate,
            Instant tfjConfirmedAt,
            String reversalOutcome,
            String reversalReference,
            Instant reversalObservedAt
    ) {
        private PaymentProjectionViews.Summary toSummary() {
            PaymentProjectionViews.MaskedAccountView account =
                    debtorAccountReference == null
                            || debtorAccountMasked == null
                            ? null
                            : new PaymentProjectionViews
                                    .MaskedAccountView(
                                    debtorAccountReference,
                                    debtorAccountMasked
                            );

            return new PaymentProjectionViews.Summary(
                    paymentId,
                    paymentReference,
                    tresorPayRequestId,
                    null,
                    financialInstitutionCode,
                    account,
                    new PaymentProjectionViews.MoneyView(
                            amount,
                            currency
                    ),
                    status,
                    reasonCode,
                    receivedAt,
                    updatedAt,
                    finalizedAt
            );
        }

        private PaymentProjectionViews.Detail toDetail() {
            if (correlationId == null
                    || correlationId.isBlank()) {
                throw new PaymentPersistenceException(
                        "Payment projection has no correlation ID: "
                                + paymentId
                );
            }

            return new PaymentProjectionViews.Detail(
                    toSummary(),
                    UUID.fromString(correlationId),
                    businessVersion,
                    banking(),
                    posting(),
                    tfj(),
                    List.of(),
                    reversal()
            );
        }

        private PaymentProjectionViews.BankingVerification
                banking() {
            if (bankingVerificationId == null) {
                return null;
            }

            return new PaymentProjectionViews
                    .BankingVerification(
                    bankingVerificationId,
                    mapBankingOutcome(bankingOutcome),
                    bankingReasonCodes,
                    bankingObservedAt
            );
        }

        private PaymentProjectionViews.Posting posting() {
            if (postingOutcome == null) {
                return null;
            }

            return new PaymentProjectionViews.Posting(
                    bankPostingReference,
                    mapPostingOutcome(postingOutcome),
                    postingObservedAt
            );
        }

        private PaymentProjectionViews.Tfj tfj() {
            if (tfjStatus == null) {
                return null;
            }

            return new PaymentProjectionViews.Tfj(
                    tfjStatus,
                    tfjBusinessDate,
                    tfjConfirmedAt
            );
        }

        private PaymentProjectionViews.Reversal reversal() {
            String mappedStatus =
                    mapReversalStatus(
                            status,
                            reversalOutcome
                    );

            if ("NOT_REQUIRED".equals(mappedStatus)
                    && reversalReference == null
                    && reversalObservedAt == null) {
                return null;
            }

            return new PaymentProjectionViews.Reversal(
                    mappedStatus,
                    reversalReference,
                    reversalObservedAt
            );
        }
    }

    private static String mapBankingOutcome(String outcome) {
        if (outcome == null) {
            return null;
        }
        return switch (outcome) {
            case "VERIFIED" -> "VERIFIED";
            case "REJECTED" -> "REJECTED";
            case "INDETERMINATE" -> "DEFERRED";
            default -> "FAILED";
        };
    }

    private static String mapPostingOutcome(String outcome) {
        return switch (outcome) {
            case "COMPLETED" -> "CUT_CREDIT_CONFIRMED";
            case "DEBIT_CONFIRMED_CUT_CREDIT_PENDING" ->
                    "DEBIT_CONFIRMED";
            case "UNKNOWN" -> "UNKNOWN";
            case "REJECTED_NO_FINANCIAL_EFFECT" -> "FAILED";
            case "REVERSAL_REQUIRED" -> "PARTIAL";
            default -> "FAILED";
        };
    }

    private static String mapReversalStatus(
            String paymentStatus,
            String reversalOutcome
    ) {
        if (reversalOutcome != null) {
            return switch (reversalOutcome) {
                case "REVERSED" -> "REVERSED";
                case "UNKNOWN" -> "UNKNOWN";
                case "REJECTED", "NOT_ALLOWED" -> "FAILED";
                default -> "FAILED";
            };
        }

        return switch (paymentStatus) {
            case "REVERSAL_REQUIRED" -> "REQUIRED";
            case "REVERSAL_PENDING" -> "PENDING";
            case "REVERSAL_OUTCOME_UNKNOWN" -> "UNKNOWN";
            case "REVERSED" -> "REVERSED";
            default -> "NOT_REQUIRED";
        };
    }
}

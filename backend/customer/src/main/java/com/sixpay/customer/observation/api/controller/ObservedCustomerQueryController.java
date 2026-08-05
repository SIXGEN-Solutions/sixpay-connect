package com.sixpay.customer.observation.api.controller;

import com.sixpay.customer.observation.api.dto
        .ObservedCustomerDetailResponse;
import com.sixpay.customer.observation.api.dto
        .ObservedCustomerPaymentPageResponse;
import com.sixpay.customer.observation.api.dto
        .ObservedCustomerSearchPageResponse;
import com.sixpay.customer.observation.api.mapper
        .ObservedCustomerQueryApiMapper;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryObservation;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryOperation;
import com.sixpay.customer.observation.application.port.input.query
        .GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .ListObservedCustomerPaymentsUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .SearchObservedCustomersUseCase;
import com.sixpay.customer.observation.application.query
        .GetObservedCustomerQuery;
import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSort;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnWebApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/observed-customers")
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnBean({
        SearchObservedCustomersUseCase.class,
        GetObservedCustomerUseCase.class,
        ListObservedCustomerPaymentsUseCase.class,
        ObservedCustomerQueryObservation.class
})
public final class ObservedCustomerQueryController {

    public static final String CORRELATION_HEADER =
            "X-Correlation-ID";

    private static final String REQUIRED_SCOPE =
            "hasAuthority('SCOPE_observed-customer.read')";

    private final SearchObservedCustomersUseCase searchUseCase;
    private final GetObservedCustomerUseCase getUseCase;
    private final ListObservedCustomerPaymentsUseCase paymentsUseCase;
    private final ObservedCustomerQueryApiMapper mapper;
    private final ObservedCustomerQueryObservation observation;
    private final Clock clock;

    public ObservedCustomerQueryController(
            SearchObservedCustomersUseCase searchUseCase,
            GetObservedCustomerUseCase getUseCase,
            ListObservedCustomerPaymentsUseCase paymentsUseCase,
            ObservedCustomerQueryApiMapper mapper,
            ObservedCustomerQueryObservation observation,
            Clock clock
    ) {
        this.searchUseCase = Objects.requireNonNull(
                searchUseCase,
                "searchUseCase is required"
        );
        this.getUseCase = Objects.requireNonNull(
                getUseCase,
                "getUseCase is required"
        );
        this.paymentsUseCase = Objects.requireNonNull(
                paymentsUseCase,
                "paymentsUseCase is required"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
        this.observation = Objects.requireNonNull(
                observation,
                "observation is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
    }

    @GetMapping
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerSearchPageResponse search(
            @RequestHeader(CORRELATION_HEADER)
            String correlationId,
            HttpServletResponse response,

            @RequestParam(required = false)
            String normalizedNiu,
            @RequestParam(required = false)
            String legalName,
            @RequestParam(required = false)
            String financialInstitutionCode,
            @RequestParam(required = false)
            ObservedPaymentStatus lastPaymentStatus,
            @RequestParam(required = false)
            String lastFailureReasonCode,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant firstObservedFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant firstObservedTo,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant lastObservedFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant lastObservedTo,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant paymentFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant paymentTo,

            @RequestParam(required = false)
            ObservedCustomerSort sort,

            @RequestParam(required = false)
            String cursor,

            @RequestParam(required = false)
            Integer size,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant snapshotAt
    ) {
        String normalizedCorrelationId =
                requireCorrelationId(correlationId);

        response.setHeader(
                CORRELATION_HEADER,
                normalizedCorrelationId
        );

        int effectiveSize = resolveSearchSize(size);

        SearchObservedCustomersQuery query =
                new SearchObservedCustomersQuery(
                        normalizedNiu,
                        legalName,
                        financialInstitutionCode,
                        lastPaymentStatus,
                        lastFailureReasonCode,
                        firstObservedFrom,
                        firstObservedTo,
                        lastObservedFrom,
                        lastObservedTo,
                        paymentFrom,
                        paymentTo,
                        sort,
                        toCursor(cursor),
                        effectiveSize,
                        resolveSnapshot(snapshotAt)
                );

        return observation.observe(
                ObservedCustomerQueryOperation.SEARCH,
                normalizedCorrelationId,
                null,
                effectiveSize,
                () -> mapper.toResponse(
                        searchUseCase.search(query)
                ),
                page ->
                        ObservedCustomerQueryObservation
                                .ResultMetadata
                                .page(page.hasMore())
        );
    }

    @GetMapping("/{observedCustomerId}")
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerDetailResponse get(
            @RequestHeader(CORRELATION_HEADER)
            String correlationId,
            HttpServletResponse response,
            @PathVariable UUID observedCustomerId
    ) {
        String normalizedCorrelationId =
                requireCorrelationId(correlationId);

        response.setHeader(
                CORRELATION_HEADER,
                normalizedCorrelationId
        );

        GetObservedCustomerQuery query =
                new GetObservedCustomerQuery(
                        ObservedCustomerId.of(
                                observedCustomerId
                        )
                );

        return observation.observe(
                ObservedCustomerQueryOperation.GET,
                normalizedCorrelationId,
                observedCustomerId,
                null,
                () -> mapper.toResponse(
                        getUseCase.get(query)
                ),
                detail ->
                        ObservedCustomerQueryObservation
                                .ResultMetadata
                                .none()
        );
    }

    @GetMapping("/{observedCustomerId}/payments")
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerPaymentPageResponse listPayments(
            @RequestHeader(CORRELATION_HEADER)
            String correlationId,
            HttpServletResponse response,
            @PathVariable UUID observedCustomerId,

            @RequestParam(required = false)
            ObservedPaymentStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant createdFrom,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant createdTo,

            @RequestParam(required = false)
            String cursor,

            @RequestParam(required = false)
            Integer size,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant snapshotAt
    ) {
        String normalizedCorrelationId =
                requireCorrelationId(correlationId);

        response.setHeader(
                CORRELATION_HEADER,
                normalizedCorrelationId
        );

        int effectiveSize = resolvePaymentSize(size);

        ListObservedCustomerPaymentsQuery query =
                new ListObservedCustomerPaymentsQuery(
                        ObservedCustomerId.of(
                                observedCustomerId
                        ),
                        status,
                        createdFrom,
                        createdTo,
                        toCursor(cursor),
                        effectiveSize,
                        resolveSnapshot(snapshotAt)
                );

        return observation.observe(
                ObservedCustomerQueryOperation.LIST_PAYMENTS,
                normalizedCorrelationId,
                observedCustomerId,
                effectiveSize,
                () -> mapper.toResponse(
                        paymentsUseCase.listPayments(query)
                ),
                page ->
                        ObservedCustomerQueryObservation
                                .ResultMetadata
                                .page(page.hasMore())
        );
    }

    private Instant resolveSnapshot(
            Instant requestedSnapshot
    ) {
        return requestedSnapshot == null
                ? clock.instant()
                : requestedSnapshot;
    }

    private static int resolveSearchSize(
            Integer requestedSize
    ) {
        return requestedSize == null
                ? SearchObservedCustomersQuery.DEFAULT_SIZE
                : requestedSize;
    }

    private static int resolvePaymentSize(
            Integer requestedSize
    ) {
        return requestedSize == null
                ? ListObservedCustomerPaymentsQuery.DEFAULT_SIZE
                : requestedSize;
    }

    private static ObservedCustomerCursor toCursor(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "cursor must not be blank"
            );
        }

        return new ObservedCustomerCursor(normalized);
    }

    private static String requireCorrelationId(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID must not be blank"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID must not exceed "
                            + "150 characters"
            );
        }

        return normalized;
    }
}

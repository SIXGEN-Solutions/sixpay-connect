package com.sixpay.customer.observation.api.controller;

import com.sixpay.customer.observation.api.dto.ObservedCustomerDetailResponse;
import com.sixpay.customer.observation.api.dto.ObservedCustomerPaymentPageResponse;
import com.sixpay.customer.observation.api.dto.ObservedCustomerSearchPageResponse;
import com.sixpay.customer.observation.api.mapper.ObservedCustomerQueryApiMapper;
import com.sixpay.customer.observation.api.observability.ObservedCustomerQueryObservation;
import com.sixpay.customer.observation.api.observability.ObservedCustomerQueryOperation;
import com.sixpay.customer.observation.application.port.input.query.GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.port.input.query.ListObservedCustomerPaymentsUseCase;
import com.sixpay.customer.observation.application.port.input.query.SearchObservedCustomersUseCase;
import com.sixpay.customer.observation.application.query.GetObservedCustomerQuery;
import com.sixpay.customer.observation.application.query.ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query.ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query.ObservedCustomerSort;
import com.sixpay.customer.observation.application.query.SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/observed-customers")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean({
        SearchObservedCustomersUseCase.class,
        GetObservedCustomerUseCase.class,
        ListObservedCustomerPaymentsUseCase.class,
        ObservedCustomerQueryObservation.class
})
public final class ObservedCustomerQueryController {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";

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
        this.searchUseCase = Objects.requireNonNull(searchUseCase);
        this.getUseCase = Objects.requireNonNull(getUseCase);
        this.paymentsUseCase = Objects.requireNonNull(paymentsUseCase);
        this.mapper = Objects.requireNonNull(mapper);
        this.observation = Objects.requireNonNull(observation);
        this.clock = Objects.requireNonNull(clock);
    }

    @GetMapping
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerSearchPageResponse search(
            @RequestHeader(CORRELATION_HEADER) String correlationId,
            HttpServletResponse response,
            @RequestParam(name = "niu", required = false) String normalizedNiu,
            @RequestParam(required = false) String legalName,
            @RequestParam(required = false) String financialInstitutionCode,
            @RequestParam(required = false) ObservedPaymentStatus lastPaymentStatus,
            @RequestParam(required = false) String lastFailureReasonCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant firstObservedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant firstObservedTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant lastObservedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant lastObservedTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant paymentFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant paymentTo,
            @RequestParam(required = false) ObservedCustomerSort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant snapshotAt
    ) {
        String correlation = requireCorrelationId(correlationId);
        response.setHeader(CORRELATION_HEADER, correlation);
        int effectiveSize = searchSize(size);

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
                        cursor(cursor),
                        effectiveSize,
                        snapshot(snapshotAt)
                );

        return observation.observe(
                ObservedCustomerQueryOperation.SEARCH,
                correlation,
                null,
                effectiveSize,
                () -> mapper.toResponse(searchUseCase.search(query)),
                page -> ObservedCustomerQueryObservation
                        .ResultMetadata.page(page.hasMore())
        );
    }

    @GetMapping("/{observedCustomerId}")
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerDetailResponse get(
            @RequestHeader(CORRELATION_HEADER) String correlationId,
            HttpServletResponse response,
            @PathVariable UUID observedCustomerId
    ) {
        String correlation = requireCorrelationId(correlationId);
        response.setHeader(CORRELATION_HEADER, correlation);

        GetObservedCustomerQuery query = new GetObservedCustomerQuery(
                ObservedCustomerId.of(observedCustomerId)
        );

        return observation.observe(
                ObservedCustomerQueryOperation.GET,
                correlation,
                observedCustomerId,
                null,
                () -> mapper.toResponse(getUseCase.get(query)),
                ignored -> ObservedCustomerQueryObservation
                        .ResultMetadata.none()
        );
    }

    @GetMapping("/{observedCustomerId}/payments")
    @PreAuthorize(REQUIRED_SCOPE)
    public ObservedCustomerPaymentPageResponse listPayments(
            @RequestHeader(CORRELATION_HEADER) String correlationId,
            HttpServletResponse response,
            @PathVariable UUID observedCustomerId,
            @RequestParam(required = false) ObservedPaymentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdTo,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant snapshotAt
    ) {
        String correlation = requireCorrelationId(correlationId);
        response.setHeader(CORRELATION_HEADER, correlation);
        int effectiveSize = paymentSize(size);

        ListObservedCustomerPaymentsQuery query =
                new ListObservedCustomerPaymentsQuery(
                        ObservedCustomerId.of(observedCustomerId),
                        status,
                        createdFrom,
                        createdTo,
                        cursor(cursor),
                        effectiveSize,
                        snapshot(snapshotAt)
                );

        return observation.observe(
                ObservedCustomerQueryOperation.LIST_PAYMENTS,
                correlation,
                observedCustomerId,
                effectiveSize,
                () -> mapper.toResponse(
                        paymentsUseCase.listPayments(query)
                ),
                page -> ObservedCustomerQueryObservation
                        .ResultMetadata.page(page.hasMore())
        );
    }

    private Instant snapshot(Instant requested) {
        return requested == null ? clock.instant() : requested;
    }

    private static int searchSize(Integer requested) {
        return requested == null
                ? SearchObservedCustomersQuery.DEFAULT_SIZE
                : requested;
    }

    private static int paymentSize(Integer requested) {
        return requested == null
                ? ListObservedCustomerPaymentsQuery.DEFAULT_SIZE
                : requested;
    }

    private static ObservedCustomerCursor cursor(String value) {
        return value == null
                ? null
                : new ObservedCustomerCursor(value.strip());
    }

    private static String requireCorrelationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID must not be blank"
            );
        }

        try {
            return UUID.fromString(value.strip()).toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID must be a valid UUID",
                    exception
            );
        }
    }
}

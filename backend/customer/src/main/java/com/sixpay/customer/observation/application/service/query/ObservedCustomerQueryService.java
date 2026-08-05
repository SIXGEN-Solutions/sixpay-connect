package com.sixpay.customer.observation.application.service.query;

import com.sixpay.customer.observation.application.exception
        .ObservedCustomerNotFoundException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import com.sixpay.customer.observation.application.port.input.query
        .GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .ListObservedCustomerPaymentsUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .SearchObservedCustomersUseCase;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerCursorCodec;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerPaymentQueryRepository;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerQueryRepository;
import com.sixpay.customer.observation.application.query
        .GetObservedCustomerQuery;
import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPage;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Framework-free orchestration service for the internal
 * Observed Customer query capability.
 *
 * <p>The service reads dedicated query projections only.
 * It never loads or reconstitutes the mutable
 * ObservedCustomer aggregate.</p>
 */
public final class ObservedCustomerQueryService
        implements SearchObservedCustomersUseCase,
        GetObservedCustomerUseCase,
        ListObservedCustomerPaymentsUseCase {

    private final ObservedCustomerQueryRepository customerQueries;
    private final ObservedCustomerPaymentQueryRepository paymentQueries;
    private final ObservedCustomerCursorCodec cursorCodec;

    public ObservedCustomerQueryService(
            ObservedCustomerQueryRepository customerQueries,
            ObservedCustomerPaymentQueryRepository paymentQueries,
            ObservedCustomerCursorCodec cursorCodec
    ) {
        this.customerQueries = Objects.requireNonNull(
                customerQueries,
                "customerQueries is required"
        );

        this.paymentQueries = Objects.requireNonNull(
                paymentQueries,
                "paymentQueries is required"
        );

        this.cursorCodec = Objects.requireNonNull(
                cursorCodec,
                "cursorCodec is required"
        );
    }

    @Override
    public ObservedCustomerSearchPage search(
            SearchObservedCustomersQuery query
    ) {
        SearchObservedCustomersQuery requested =
                Objects.requireNonNull(
                        query,
                        "query is required"
                );

        /*
         * Cursor errors representing an invalid client cursor must remain
         * IllegalArgumentException so the API layer can return HTTP 400.
         *
         * Repository failures and incoherent repository responses are
         * translated to query-unavailable.
         */
        SearchObservedCustomersQuery canonical =
                resolveSearchCursor(requested);

        validateSearchQuery(canonical);

        return executeRead(
                "Observed Customer search is unavailable",
                () -> {
                    ObservedCustomerSearchPage page =
                            customerQueries.search(canonical);

                    validateSearchPage(
                            canonical,
                            page
                    );

                    return page;
                }
        );
    }

    @Override
    public ObservedCustomerDetailView get(
            GetObservedCustomerQuery query
    ) {
        GetObservedCustomerQuery requested =
                Objects.requireNonNull(
                        query,
                        "query is required"
                );

        return executeRead(
                "Observed Customer detail is unavailable",
                () -> customerQueries
                        .findDetailById(
                                requested.observedCustomerId()
                        )
                        .orElseThrow(() ->
                                new ObservedCustomerNotFoundException(
                                        requested.observedCustomerId()
                                )
                        )
        );
    }

    @Override
    public ObservedCustomerPaymentPage listPayments(
            ListObservedCustomerPaymentsQuery query
    ) {
        ListObservedCustomerPaymentsQuery requested =
                Objects.requireNonNull(
                        query,
                        "query is required"
                );

        ListObservedCustomerPaymentsQuery canonical =
                resolvePaymentCursor(requested);

        validatePaymentQuery(canonical);

        return executeRead(
                "Observed Customer payments are unavailable",
                () -> {
                    if (!customerQueries.existsById(
                            canonical.observedCustomerId()
                    )) {
                        throw new ObservedCustomerNotFoundException(
                                canonical.observedCustomerId()
                        );
                    }

                    ObservedCustomerPaymentPage page =
                            paymentQueries.findByCustomer(
                                    canonical
                            );

                    validatePaymentPage(
                            canonical,
                            page
                    );

                    return page;
                }
        );
    }

    private SearchObservedCustomersQuery resolveSearchCursor(
            SearchObservedCustomersQuery query
    ) {
        try {
            return Objects.requireNonNull(
                    cursorCodec.resolveSearch(query),
                    "cursor codec returned no search query"
            );
        } catch (IllegalArgumentException exception) {
            /*
             * Invalid, altered or incompatible cursor:
             * preserve as a client validation error.
             */
            throw exception;
        } catch (RuntimeException exception) {
            throw new ObservedCustomerQueryUnavailableException(
                    "Observed Customer cursor validation "
                            + "is unavailable",
                    exception
            );
        }
    }

    private ListObservedCustomerPaymentsQuery resolvePaymentCursor(
            ListObservedCustomerPaymentsQuery query
    ) {
        try {
            return Objects.requireNonNull(
                    cursorCodec.resolvePayments(query),
                    "cursor codec returned no payment query"
            );
        } catch (IllegalArgumentException exception) {
            /*
             * Invalid, altered or incompatible cursor:
             * preserve as a client validation error.
             */
            throw exception;
        } catch (RuntimeException exception) {
            throw new ObservedCustomerQueryUnavailableException(
                    "Observed Customer payment cursor validation "
                            + "is unavailable",
                    exception
            );
        }
    }

    private static void validateSearchQuery(
            SearchObservedCustomersQuery query
    ) {
        requireSize(
                query.size(),
                SearchObservedCustomersQuery.MAX_SIZE
        );

        requireOrdered(
                query.firstObservedFrom(),
                query.firstObservedTo(),
                "firstObserved"
        );

        requireOrdered(
                query.lastObservedFrom(),
                query.lastObservedTo(),
                "lastObserved"
        );

        requireOrdered(
                query.paymentFrom(),
                query.paymentTo(),
                "payment"
        );

        Objects.requireNonNull(
                query.snapshotAt(),
                "snapshotAt is required"
        );
    }

    private static void validatePaymentQuery(
            ListObservedCustomerPaymentsQuery query
    ) {
        requireSize(
                query.size(),
                ListObservedCustomerPaymentsQuery.MAX_SIZE
        );

        requireOrdered(
                query.createdFrom(),
                query.createdTo(),
                "created"
        );

        Objects.requireNonNull(
                query.snapshotAt(),
                "snapshotAt is required"
        );
    }

    private static void validateSearchPage(
            SearchObservedCustomersQuery query,
            ObservedCustomerSearchPage page
    ) {
        Objects.requireNonNull(
                page,
                "customer query repository returned no page"
        );

        if (!query.snapshotAt().equals(
                page.snapshotAt()
        )) {
            throw new IllegalStateException(
                    "search page snapshot does not match "
                            + "query snapshot"
            );
        }

        if (page.size() > query.size()) {
            throw new IllegalStateException(
                    "search page exceeds requested size"
            );
        }
    }

    private static void validatePaymentPage(
            ListObservedCustomerPaymentsQuery query,
            ObservedCustomerPaymentPage page
    ) {
        Objects.requireNonNull(
                page,
                "payment query repository returned no page"
        );

        if (!query.snapshotAt().equals(
                page.snapshotAt()
        )) {
            throw new IllegalStateException(
                    "payment page snapshot does not match "
                            + "query snapshot"
            );
        }

        if (page.size() > query.size()) {
            throw new IllegalStateException(
                    "payment page exceeds requested size"
            );
        }
    }

    private static void requireSize(
            int size,
            int maximum
    ) {
        if (size < 1 || size > maximum) {
            throw new IllegalArgumentException(
                    "size must be between 1 and "
                            + maximum
            );
        }
    }

    private static void requireOrdered(
            java.time.Instant from,
            java.time.Instant to,
            String field
    ) {
        if (from != null
                && to != null
                && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    field + " from must not be after to"
            );
        }
    }

    private static <T> T executeRead(
            String unavailableMessage,
            Supplier<T> operation
    ) {
        try {
            return operation.get();
        } catch (ObservedCustomerNotFoundException
                 | ObservedCustomerQueryUnavailableException
                 | IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ObservedCustomerQueryUnavailableException(
                    unavailableMessage,
                    exception
            );
        }
    }
}
package com.sixpay.customer.observation.application.service.query;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
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
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPage;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentSlice;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchSlice;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Framework-free query orchestration over dedicated read projections.
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

        ObservedCustomerSearchCriteria criteria =
                decodeSearch(requested);

        return executeRead(
                "Observed Customer search is unavailable",
                () -> toSearchPage(
                        criteria,
                        customerQueries.search(criteria)
                )
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

        ObservedCustomerPaymentCriteria criteria =
                decodePayments(requested);

        return executeRead(
                "Observed Customer payments are unavailable",
                () -> {
                    if (!customerQueries.existsById(
                            criteria.observedCustomerId()
                    )) {
                        throw new ObservedCustomerNotFoundException(
                                criteria.observedCustomerId()
                        );
                    }

                    return toPaymentPage(
                            criteria,
                            paymentQueries.findByCustomerId(
                                    criteria
                            )
                    );
                }
        );
    }

    private ObservedCustomerSearchCriteria decodeSearch(
            SearchObservedCustomersQuery query
    ) {
        try {
            return Objects.requireNonNull(
                    cursorCodec.decodeSearch(query),
                    "cursor codec returned no search criteria"
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ObservedCustomerQueryUnavailableException(
                    "Observed Customer cursor validation "
                            + "is unavailable",
                    exception
            );
        }
    }

    private ObservedCustomerPaymentCriteria decodePayments(
            ListObservedCustomerPaymentsQuery query
    ) {
        try {
            return Objects.requireNonNull(
                    cursorCodec.decodePayments(query),
                    "cursor codec returned no payment criteria"
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ObservedCustomerQueryUnavailableException(
                    "Observed Customer payment cursor validation "
                            + "is unavailable",
                    exception
            );
        }
    }

    private ObservedCustomerSearchPage toSearchPage(
            ObservedCustomerSearchCriteria criteria,
            ObservedCustomerSearchSlice slice
    ) {
        Objects.requireNonNull(
                slice,
                "customer query repository returned no slice"
        );

        if (slice.items().size() > criteria.size()) {
            throw new IllegalStateException(
                    "search slice exceeds requested size"
            );
        }

        ObservedCustomerCursor nextCursor =
                slice.hasMore()
                        ? cursorCodec.encodeSearch(
                                criteria,
                                slice.nextPosition()
                        )
                        : null;

        return new ObservedCustomerSearchPage(
                slice.items(),
                slice.items().size(),
                slice.hasMore(),
                nextCursor,
                criteria.snapshotAt()
        );
    }

    private ObservedCustomerPaymentPage toPaymentPage(
            ObservedCustomerPaymentCriteria criteria,
            ObservedCustomerPaymentSlice slice
    ) {
        Objects.requireNonNull(
                slice,
                "payment query repository returned no slice"
        );

        if (slice.items().size() > criteria.size()) {
            throw new IllegalStateException(
                    "payment slice exceeds requested size"
            );
        }

        ObservedCustomerCursor nextCursor =
                slice.hasMore()
                        ? cursorCodec.encodePayments(
                                criteria,
                                slice.nextPosition()
                        )
                        : null;

        return new ObservedCustomerPaymentPage(
                slice.items(),
                slice.items().size(),
                slice.hasMore(),
                nextCursor,
                criteria.snapshotAt()
        );
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

package com.sixpay.customer.observation.application.service.query;

import com.sixpay.customer.observation.application.exception
        .ObservedCustomerNotFoundException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
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
        .ObservedCustomerPaymentPage;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservedCustomerQueryServiceTest {

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-04T20:00:00Z");

    @Test
    void searchResolvesCursorBeforeCallingReadRepository() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(
                        new ObservedCustomerCursor("opaque")
                );

        SearchObservedCustomersQuery canonical =
                searchQuery(
                        new ObservedCustomerCursor("validated")
                );

        ObservedCustomerSearchPage page =
                new ObservedCustomerSearchPage(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        when(codec.resolveSearch(requested))
                .thenReturn(canonical);
        when(customers.search(canonical))
                .thenReturn(page);

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        assertSame(page, service.search(requested));

        verify(codec).resolveSearch(requested);
        verify(customers).search(canonical);
        verify(payments, never())
                .findByCustomer(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void detailNotFoundIsDistinctFromTemporaryFailure() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        when(customers.findDetailById(customerId()))
                .thenReturn(Optional.empty());

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        assertThrows(
                ObservedCustomerNotFoundException.class,
                () -> service.get(
                        new GetObservedCustomerQuery(
                                customerId()
                        )
                )
        );

        when(customers.findDetailById(customerId()))
                .thenThrow(
                        new IllegalStateException(
                                "database unavailable"
                        )
                );

        assertThrows(
                ObservedCustomerQueryUnavailableException.class,
                () -> service.get(
                        new GetObservedCustomerQuery(
                                customerId()
                        )
                )
        );
    }

    @Test
    void paymentListValidatesCustomerExistenceAndSnapshot() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        ListObservedCustomerPaymentsQuery requested =
                paymentQuery();

        when(codec.resolvePayments(requested))
                .thenReturn(requested);
        when(customers.existsById(customerId()))
                .thenReturn(true);

        ObservedCustomerPaymentPage page =
                new ObservedCustomerPaymentPage(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        when(payments.findByCustomer(requested))
                .thenReturn(page);

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        assertSame(
                page,
                service.listPayments(requested)
        );

        verify(customers).existsById(customerId());
        verify(payments).findByCustomer(requested);
    }

    @Test
    void paymentListDoesNotQueryRowsForUnknownCustomer() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        ListObservedCustomerPaymentsQuery requested =
                paymentQuery();

        when(codec.resolvePayments(requested))
                .thenReturn(requested);
        when(customers.existsById(customerId()))
                .thenReturn(false);

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        assertThrows(
                ObservedCustomerNotFoundException.class,
                () -> service.listPayments(requested)
        );

        verify(payments, never())
                .findByCustomer(requested);
    }

    @Test
    void repositoryCannotReturnDifferentSnapshotOrOversizedPage() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        when(codec.resolveSearch(requested))
                .thenReturn(requested);

        when(customers.search(requested))
                .thenReturn(
                        new ObservedCustomerSearchPage(
                                List.of(),
                                0,
                                false,
                                null,
                                SNAPSHOT.plusSeconds(1)
                        )
                );

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        assertThrows(
                ObservedCustomerQueryUnavailableException.class,
                () -> service.search(requested)
        );
    }

    @Test
    void explicitUnavailableExceptionIsPreserved() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        when(codec.resolveSearch(requested))
                .thenReturn(requested);
        when(customers.search(requested))
                .thenThrow(
                        new ObservedCustomerQueryUnavailableException(
                                "temporary"
                        )
                );

        ObservedCustomerQueryService service =
                service(customers, payments, codec);

        ObservedCustomerQueryUnavailableException exception =
                assertThrows(
                        ObservedCustomerQueryUnavailableException.class,
                        () -> service.search(requested)
                );

        assertEquals("temporary", exception.getMessage());
    }

    private static ObservedCustomerQueryService service(
            ObservedCustomerQueryRepository customers,
            ObservedCustomerPaymentQueryRepository payments,
            ObservedCustomerCursorCodec codec
    ) {
        return new ObservedCustomerQueryService(
                customers,
                payments,
                codec
        );
    }

    private static SearchObservedCustomersQuery searchQuery(
            ObservedCustomerCursor cursor
    ) {
        return new SearchObservedCustomersQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                cursor,
                50,
                SNAPSHOT
        );
    }

    private static ListObservedCustomerPaymentsQuery paymentQuery() {
        return new ListObservedCustomerPaymentsQuery(
                customerId(),
                null,
                null,
                null,
                null,
                50,
                SNAPSHOT
        );
    }

    private static ObservedCustomerId customerId() {
        return ObservedCustomerId.of(
                UUID.fromString(
                        "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                )
        );
    }
}

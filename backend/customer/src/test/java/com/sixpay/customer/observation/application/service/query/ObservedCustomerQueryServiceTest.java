package com.sixpay.customer.observation.application.service.query;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
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
import com.sixpay.customer.observation.application.query.*;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservedCustomerQueryServiceTest {

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-04T20:00:00Z");

    private static final Instant LAST_SORT_VALUE =
            Instant.parse("2026-08-04T19:30:00Z");

    private static final Instant LAST_PAYMENT_CREATED_AT =
            Instant.parse("2026-08-04T19:15:00Z");

    private static final UUID LAST_PAYMENT_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    @Test
    void searchDecodesCursorBeforeCallingReadRepository() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(
                        new ObservedCustomerCursor(
                                "opaque-search-cursor"
                        )
                );

        ObservedCustomerSearchCriteria criteria =
                searchCriteria(
                        new ObservedCustomerSearchPosition(
                                LAST_SORT_VALUE,
                                customerId()
                        )
                );

        ObservedCustomerSearchSlice slice =
                new ObservedCustomerSearchSlice(
                        List.of(),
                        false,
                        null
                );

        when(codec.decodeSearch(requested))
                .thenReturn(criteria);

        when(customers.search(criteria))
                .thenReturn(slice);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        ObservedCustomerSearchPage page =
                service.search(requested);

        assertEquals(0, page.size());
        assertFalse(page.hasMore());
        assertEquals(SNAPSHOT, page.snapshotAt());
        assertEquals(null, page.nextCursor());

        verify(codec).decodeSearch(requested);
        verify(customers).search(criteria);

        verify(codec, never())
                .encodeSearch(
                        any(),
                        any()
                );

        verify(payments, never())
                .findByCustomerId(any());
    }

    @Test
    void searchCreatesSignedCursorWhenSliceHasMore() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        ObservedCustomerSearchCriteria criteria =
                searchCriteria(null);

        ObservedCustomerSearchPosition nextPosition =
                new ObservedCustomerSearchPosition(
                        LAST_SORT_VALUE,
                        customerId()
                );

        ObservedCustomerSearchSlice slice =
                new ObservedCustomerSearchSlice(
                        List.of(),
                        true,
                        nextPosition
                );

        ObservedCustomerCursor nextCursor =
                new ObservedCustomerCursor(
                        "signed-next-search-cursor"
                );

        when(codec.decodeSearch(requested))
                .thenReturn(criteria);

        when(customers.search(criteria))
                .thenReturn(slice);

        when(codec.encodeSearch(
                criteria,
                nextPosition
        )).thenReturn(nextCursor);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        ObservedCustomerSearchPage page =
                service.search(requested);

        assertTrue(page.hasMore());
        assertSame(
                nextCursor,
                page.nextCursor()
        );
        assertEquals(
                SNAPSHOT,
                page.snapshotAt()
        );

        verify(codec).encodeSearch(
                criteria,
                nextPosition
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
                service(
                        customers,
                        payments,
                        codec
                );

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
    void paymentListDecodesCursorAndValidatesCustomerExistence() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        ListObservedCustomerPaymentsQuery requested =
                paymentQuery(
                        new ObservedCustomerCursor(
                                "opaque-payment-cursor"
                        )
                );

        ObservedCustomerPaymentCriteria criteria =
                paymentCriteria(
                        new ObservedCustomerPaymentPosition(
                                LAST_PAYMENT_CREATED_AT,
                                LAST_PAYMENT_ID
                        )
                );

        ObservedCustomerPaymentSlice slice =
                new ObservedCustomerPaymentSlice(
                        List.of(),
                        false,
                        null
                );

        when(codec.decodePayments(requested))
                .thenReturn(criteria);

        when(customers.existsById(customerId()))
                .thenReturn(true);

        when(payments.findByCustomerId(criteria))
                .thenReturn(slice);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        ObservedCustomerPaymentPage page =
                service.listPayments(requested);

        assertEquals(0, page.size());
        assertFalse(page.hasMore());
        assertEquals(SNAPSHOT, page.snapshotAt());

        verify(codec).decodePayments(requested);
        verify(customers).existsById(customerId());
        verify(payments).findByCustomerId(criteria);

        verify(codec, never())
                .encodePayments(
                        any(),
                        any()
                );
    }

    @Test
    void paymentListCreatesSignedCursorWhenSliceHasMore() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        ListObservedCustomerPaymentsQuery requested =
                paymentQuery(null);

        ObservedCustomerPaymentCriteria criteria =
                paymentCriteria(null);

        ObservedCustomerPaymentPosition nextPosition =
                new ObservedCustomerPaymentPosition(
                        LAST_PAYMENT_CREATED_AT,
                        LAST_PAYMENT_ID
                );

        ObservedCustomerPaymentSlice slice =
                new ObservedCustomerPaymentSlice(
                        List.of(),
                        true,
                        nextPosition
                );

        ObservedCustomerCursor nextCursor =
                new ObservedCustomerCursor(
                        "signed-next-payment-cursor"
                );

        when(codec.decodePayments(requested))
                .thenReturn(criteria);

        when(customers.existsById(customerId()))
                .thenReturn(true);

        when(payments.findByCustomerId(criteria))
                .thenReturn(slice);

        when(codec.encodePayments(
                criteria,
                nextPosition
        )).thenReturn(nextCursor);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        ObservedCustomerPaymentPage page =
                service.listPayments(requested);

        assertTrue(page.hasMore());
        assertSame(
                nextCursor,
                page.nextCursor()
        );

        verify(codec).encodePayments(
                criteria,
                nextPosition
        );
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
                paymentQuery(null);

        ObservedCustomerPaymentCriteria criteria =
                paymentCriteria(null);

        when(codec.decodePayments(requested))
                .thenReturn(criteria);

        when(customers.existsById(customerId()))
                .thenReturn(false);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        assertThrows(
                ObservedCustomerNotFoundException.class,
                () -> service.listPayments(requested)
        );

        verify(payments, never())
                .findByCustomerId(any());

        verify(codec, never())
                .encodePayments(
                        any(),
                        any()
                );
    }

    @Test
    void oversizedSearchSliceBecomesUnavailable() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        ObservedCustomerSearchCriteria criteria =
                new ObservedCustomerSearchCriteria(
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
                        ObservedCustomerSort
                                .LAST_OBSERVED_AT_DESC,
                        1,
                        SNAPSHOT,
                        null
                );

        ObservedCustomerSearchSlice oversized =
                new ObservedCustomerSearchSlice(
                        List.of(
                                mock(
                                        ObservedCustomerSummaryView.class
                                ),
                                mock(
                                        ObservedCustomerSummaryView.class
                                )
                        ),
                        false,
                        null
                );

        when(codec.decodeSearch(requested))
                .thenReturn(criteria);

        when(customers.search(criteria))
                .thenReturn(oversized);

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        assertThrows(
                ObservedCustomerQueryUnavailableException.class,
                () -> service.search(requested)
        );
    }

    @Test
    void invalidCursorRemainsClientValidationError() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(
                        new ObservedCustomerCursor(
                                "altered-cursor"
                        )
                );

        when(codec.decodeSearch(requested))
                .thenThrow(
                        new InvalidObservedCustomerCursorException(
                                "cursor signature is invalid"
                        )
                );

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        assertThrows(
                InvalidObservedCustomerCursorException.class,
                () -> service.search(requested)
        );

        verify(customers, never())
                .search(any());
    }

    @Test
    void unexpectedCursorInfrastructureFailureBecomesUnavailable() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        when(codec.decodeSearch(requested))
                .thenThrow(
                        new IllegalStateException(
                                "HMAC provider unavailable"
                        )
                );

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        assertThrows(
                ObservedCustomerQueryUnavailableException.class,
                () -> service.search(requested)
        );

        verify(customers, never())
                .search(any());
    }

    @Test
    void explicitUnavailableRepositoryExceptionIsPreserved() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);

        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);

        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery requested =
                searchQuery(null);

        ObservedCustomerSearchCriteria criteria =
                searchCriteria(null);

        when(codec.decodeSearch(requested))
                .thenReturn(criteria);

        when(customers.search(criteria))
                .thenThrow(
                        new ObservedCustomerQueryUnavailableException(
                                "temporary"
                        )
                );

        ObservedCustomerQueryService service =
                service(
                        customers,
                        payments,
                        codec
                );

        ObservedCustomerQueryUnavailableException exception =
                assertThrows(
                        ObservedCustomerQueryUnavailableException.class,
                        () -> service.search(requested)
                );

        assertEquals(
                "temporary",
                exception.getMessage()
        );
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
                ObservedCustomerSort.LAST_OBSERVED_AT_DESC,
                cursor,
                50,
                SNAPSHOT
        );
    }

    private static ObservedCustomerSearchCriteria searchCriteria(
            ObservedCustomerSearchPosition position
    ) {
        return new ObservedCustomerSearchCriteria(
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
                ObservedCustomerSort.LAST_OBSERVED_AT_DESC,
                50,
                SNAPSHOT,
                position
        );
    }

    private static ListObservedCustomerPaymentsQuery paymentQuery(
            ObservedCustomerCursor cursor
    ) {
        return new ListObservedCustomerPaymentsQuery(
                customerId(),
                null,
                null,
                null,
                cursor,
                50,
                SNAPSHOT
        );
    }

    private static ObservedCustomerPaymentCriteria paymentCriteria(
            ObservedCustomerPaymentPosition position
    ) {
        return new ObservedCustomerPaymentCriteria(
                customerId(),
                null,
                null,
                null,
                50,
                SNAPSHOT,
                position
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
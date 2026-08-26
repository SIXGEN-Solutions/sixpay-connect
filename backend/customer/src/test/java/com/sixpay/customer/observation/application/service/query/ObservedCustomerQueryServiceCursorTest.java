package com.sixpay.customer.observation.application.service.query;

import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerCursorCodec;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerPaymentQueryRepository;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerQueryRepository;
import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentSlice;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchSlice;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSort;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservedCustomerQueryServiceCursorTest {

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-04T20:00:00Z");

    @Test
    void searchBuildsNextCursorFromRepositoryPosition() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        SearchObservedCustomersQuery query = searchQuery();

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
                        50,
                        SNAPSHOT,
                        null
                );

        ObservedCustomerSearchPosition position =
                new ObservedCustomerSearchPosition(
                        SNAPSHOT.minusSeconds(60),
                        customerId()
                );

        ObservedCustomerSearchSlice slice =
                new ObservedCustomerSearchSlice(
                        List.of(),
                        true,
                        position
                );

        ObservedCustomerCursor next =
                new ObservedCustomerCursor("signed-search");

        when(codec.decodeSearch(query))
                .thenReturn(criteria);
        when(customers.search(criteria))
                .thenReturn(slice);
        when(codec.encodeSearch(criteria, position))
                .thenReturn(next);

        var page = service(
                customers,
                payments,
                codec
        ).search(query);

        assertEquals(SNAPSHOT, page.snapshotAt());
        assertEquals(next, page.nextCursor());
        assertEquals(true, page.hasMore());

        verify(codec).decodeSearch(query);
        verify(customers).search(criteria);
        verify(codec).encodeSearch(criteria, position);
    }

    @Test
    void paymentListBuildsNextCursorFromRepositoryPosition() {
        ObservedCustomerQueryRepository customers =
                mock(ObservedCustomerQueryRepository.class);
        ObservedCustomerPaymentQueryRepository payments =
                mock(ObservedCustomerPaymentQueryRepository.class);
        ObservedCustomerCursorCodec codec =
                mock(ObservedCustomerCursorCodec.class);

        ListObservedCustomerPaymentsQuery query =
                paymentQuery();

        ObservedCustomerPaymentCriteria criteria =
                new ObservedCustomerPaymentCriteria(
                        customerId(),
                        null,
                        null,
                        null,
                        50,
                        SNAPSHOT,
                        null
                );

        ObservedCustomerPaymentPosition position =
                new ObservedCustomerPaymentPosition(
                        SNAPSHOT.minusSeconds(60),
                        UUID.fromString(
                                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                        )
                );

        ObservedCustomerPaymentSlice slice =
                new ObservedCustomerPaymentSlice(
                        List.of(),
                        true,
                        position
                );

        ObservedCustomerCursor next =
                new ObservedCustomerCursor("signed-payment");

        when(codec.decodePayments(query))
                .thenReturn(criteria);
        when(customers.existsById(customerId()))
                .thenReturn(true);
        when(payments.findByCustomerId(criteria))
                .thenReturn(slice);
        when(codec.encodePayments(criteria, position))
                .thenReturn(next);

        var page = service(
                customers,
                payments,
                codec
        ).listPayments(query);

        assertEquals(SNAPSHOT, page.snapshotAt());
        assertSame(next, page.nextCursor());

        verify(codec).decodePayments(query);
        verify(payments).findByCustomerId(criteria);
        verify(codec).encodePayments(criteria, position);
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

    private static SearchObservedCustomersQuery searchQuery() {
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
                null,
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

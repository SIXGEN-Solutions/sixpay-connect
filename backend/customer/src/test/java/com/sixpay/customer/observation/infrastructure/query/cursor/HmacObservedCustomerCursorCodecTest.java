package com.sixpay.customer.observation.infrastructure.query.cursor;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSort;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacObservedCustomerCursorCodecTest {

    private static final byte[] KEY =
            "0123456789abcdef0123456789abcdef"
                    .getBytes(StandardCharsets.UTF_8);

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-04T20:00:00Z");

    private final HmacObservedCustomerCursorCodec codec =
            new HmacObservedCustomerCursorCodec(KEY);

    @Test
    void firstSearchPageProducesCriteriaWithoutPosition() {
        ObservedCustomerSearchCriteria criteria =
                codec.decodeSearch(
                        searchQuery(null)
                );

        assertEquals(
                ObservedCustomerSort.LAST_OBSERVED_AT_DESC,
                criteria.sort()
        );
        assertEquals(SNAPSHOT, criteria.snapshotAt());
        assertNull(criteria.position());
    }

    @Test
    void searchCursorRoundTripPreservesStablePosition() {
        ObservedCustomerSearchCriteria criteria =
                codec.decodeSearch(searchQuery(null));

        ObservedCustomerSearchPosition position =
                new ObservedCustomerSearchPosition(
                        Instant.parse("2026-08-04T19:30:00Z"),
                        customerId()
                );

        ObservedCustomerCursor cursor =
                codec.encodeSearch(criteria, position);

        ObservedCustomerSearchCriteria restored =
                codec.decodeSearch(
                        searchQuery(cursor)
                );

        assertEquals(position, restored.position());
        assertEquals(criteria.snapshotAt(), restored.snapshotAt());
        assertEquals(criteria.sort(), restored.sort());
        assertFalse(cursor.toString().contains(cursor.value()));
    }

    @Test
    void alteredSearchCursorIsRejected() {
        ObservedCustomerSearchCriteria criteria =
                codec.decodeSearch(searchQuery(null));

        ObservedCustomerCursor cursor =
                codec.encodeSearch(
                        criteria,
                        new ObservedCustomerSearchPosition(
                                Instant.parse(
                                        "2026-08-04T19:30:00Z"
                                ),
                                customerId()
                        )
                );

        char replacement =
                cursor.value().charAt(0) == 'A' ? 'B' : 'A';

        ObservedCustomerCursor altered =
                new ObservedCustomerCursor(
                        replacement
                                + cursor.value().substring(1)
                );

        assertThrows(
                InvalidObservedCustomerCursorException.class,
                () -> codec.decodeSearch(
                        searchQuery(altered)
                )
        );
    }

    @Test
    void searchCursorRejectsSortAndFilterMismatch() {
        ObservedCustomerSearchCriteria criteria =
                codec.decodeSearch(searchQuery(null));

        ObservedCustomerCursor cursor =
                codec.encodeSearch(
                        criteria,
                        new ObservedCustomerSearchPosition(
                                Instant.parse(
                                        "2026-08-04T19:30:00Z"
                                ),
                                customerId()
                        )
                );

        SearchObservedCustomersQuery differentSort =
                new SearchObservedCustomersQuery(
                        "M0123456",
                        "Société ABC",
                        "SIXPAY_BANK",
                        ObservedPaymentStatus.DEBITED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ObservedCustomerSort
                                .FIRST_OBSERVED_AT_ASC,
                        cursor,
                        50,
                        SNAPSHOT
                );

        assertThrows(
                InvalidObservedCustomerCursorException.class,
                () -> codec.decodeSearch(differentSort)
        );

        SearchObservedCustomersQuery differentFilter =
                new SearchObservedCustomersQuery(
                        "M9999999",
                        "Société ABC",
                        "SIXPAY_BANK",
                        ObservedPaymentStatus.DEBITED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ObservedCustomerSort
                                .LAST_OBSERVED_AT_DESC,
                        cursor,
                        50,
                        SNAPSHOT
                );

        assertThrows(
                InvalidObservedCustomerCursorException.class,
                () -> codec.decodeSearch(differentFilter)
        );
    }

    @Test
    void paymentCursorRoundTripPreservesStablePosition() {
        ObservedCustomerPaymentCriteria criteria =
                codec.decodePayments(
                        paymentQuery(null)
                );

        ObservedCustomerPaymentPosition position =
                new ObservedCustomerPaymentPosition(
                        Instant.parse("2026-08-04T19:10:00Z"),
                        UUID.fromString(
                                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                        )
                );

        ObservedCustomerCursor cursor =
                codec.encodePayments(criteria, position);

        ObservedCustomerPaymentCriteria restored =
                codec.decodePayments(
                        paymentQuery(cursor)
                );

        assertEquals(position, restored.position());
        assertEquals(
                customerId(),
                restored.observedCustomerId()
        );
        assertEquals(SNAPSHOT, restored.snapshotAt());
    }

    @Test
    void cursorDoesNotContainSensitiveQueryValuesInClear() {
        ObservedCustomerSearchCriteria criteria =
                codec.decodeSearch(searchQuery(null));

        ObservedCustomerCursor cursor =
                codec.encodeSearch(
                        criteria,
                        new ObservedCustomerSearchPosition(
                                Instant.parse(
                                        "2026-08-04T19:30:00Z"
                                ),
                                customerId()
                        )
                );

        assertFalse(cursor.value().contains("M0123456"));
        assertFalse(cursor.value().contains("Société ABC"));
        assertFalse(cursor.value().contains("SIXPAY_BANK"));
        assertNotNull(cursor.value());
    }

    @Test
    void keyMustBeAtLeastThirtyTwoBytes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HmacObservedCustomerCursorCodec(
                        "too-short".getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );
    }

    private static SearchObservedCustomersQuery searchQuery(
            ObservedCustomerCursor cursor
    ) {
        return new SearchObservedCustomersQuery(
                "M0123456",
                "Société ABC",
                "SIXPAY_BANK",
                ObservedPaymentStatus.DEBITED,
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

    private static ListObservedCustomerPaymentsQuery paymentQuery(
            ObservedCustomerCursor cursor
    ) {
        return new ListObservedCustomerPaymentsQuery(
                customerId(),
                ObservedPaymentStatus.DEBITED,
                null,
                null,
                cursor,
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

package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryModelTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-04T18:00:00Z");

    private static final Instant LAST =
            Instant.parse("2026-08-04T19:00:00Z");

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-04T20:00:00Z");

    @Test
    void searchQueryCoversContractFiltersAndDefaults() {
        SearchObservedCustomersQuery query =
                SearchObservedCustomersQuery.firstPage(
                        " M0123456 ",
                        " Société ABC ",
                        " SIXPAY_BANK ",
                        ObservedPaymentStatus.RECEIVED,
                        " ACCOUNT_NOT_FOUND ",
                        FIRST,
                        LAST,
                        FIRST,
                        LAST,
                        FIRST,
                        LAST,
                        null,
                        null,
                        SNAPSHOT
                );

        assertEquals("M0123456", query.normalizedNiu());
        assertEquals("Société ABC", query.legalName());
        assertEquals(
                "SIXPAY_BANK",
                query.financialInstitutionCode()
        );
        assertEquals(
                ObservedCustomerSort.LAST_OBSERVED_AT_DESC,
                query.sort()
        );
        assertEquals(
                SearchObservedCustomersQuery.DEFAULT_SIZE,
                query.size()
        );
        assertEquals(SNAPSHOT, query.snapshotAt());
        assertFalse(query.continuationPage());
    }

    @Test
    void continuationPageCarriesOpaqueCursorAndOriginalSnapshot() {
        ObservedCustomerCursor cursor =
                new ObservedCustomerCursor(
                        "v1.opaque.signed.cursor"
                );

        SearchObservedCustomersQuery query =
                new SearchObservedCustomersQuery(
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
                        cursor,
                        25,
                        SNAPSHOT
                );

        assertTrue(query.continuationPage());
        assertEquals(cursor, query.cursor());
        assertEquals(SNAPSHOT, query.snapshotAt());
        assertFalse(
                cursor.toString().contains(
                        "v1.opaque.signed.cursor"
                )
        );
    }

    @Test
    void searchRejectsInvalidRangesSizeAndMissingSnapshot() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchObservedCustomersQuery(
                        null, null, null, null, null,
                        LAST, FIRST,
                        null, null,
                        null, null,
                        null, null,
                        50, SNAPSHOT
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchObservedCustomersQuery(
                        null, null, null, null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        0, SNAPSHOT
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchObservedCustomersQuery(
                        null, null, null, null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        201, SNAPSHOT
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new SearchObservedCustomersQuery(
                        null, null, null, null, null,
                        null, null,
                        null, null,
                        null, null,
                        null, null,
                        50, null
                )
        );
    }

    @Test
    void paymentQueryUsesSameStableSnapshotRules() {
        ListObservedCustomerPaymentsQuery query =
                ListObservedCustomerPaymentsQuery.firstPage(
                        customerId(),
                        ObservedPaymentStatus.DEBITED,
                        FIRST,
                        LAST,
                        null,
                        SNAPSHOT
                );

        assertEquals(customerId(), query.observedCustomerId());
        assertEquals(
                ListObservedCustomerPaymentsQuery.DEFAULT_SIZE,
                query.size()
        );
        assertEquals(SNAPSHOT, query.snapshotAt());
        assertFalse(query.continuationPage());

        assertThrows(
                IllegalArgumentException.class,
                () -> ListObservedCustomerPaymentsQuery
                        .firstPage(
                                customerId(),
                                null,
                                LAST,
                                FIRST,
                                50,
                                SNAPSHOT
                        )
        );
    }

    @Test
    void viewsUseDefensiveCollectionsAndNoFingerprintField() {
        ArrayList<ObservedAccountView> accounts =
                new ArrayList<>(
                        List.of(
                                new ObservedAccountView(
                                        "account-ref-1",
                                        "•••• 1234"
                                )
                        )
                );

        ObservedInstitutionView institution =
                new ObservedInstitutionView(
                        "SIXPAY_BANK",
                        FIRST,
                        LAST,
                        accounts
                );

        accounts.clear();

        assertEquals(1, institution.accounts().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> institution.accounts().clear()
        );

        assertFalse(
                List.of(
                        ObservedAccountView.class
                                .getRecordComponents()
                ).stream().anyMatch(component ->
                        component.getName()
                                .toLowerCase()
                                .contains("fingerprint")
                )
        );
    }

    @Test
    void summaryAndDetailProtectIdentityInRendering() {
        ObservedCustomerSummaryView summary =
                summary();

        ObservedCustomerDetailView detail =
                new ObservedCustomerDetailView(
                        summary.observedCustomerId(),
                        summary.niu(),
                        summary.legalName(),
                        summary.phone(),
                        summary.email(),
                        List.of(
                                new ObservedInstitutionView(
                                        "SIXPAY_BANK",
                                        FIRST,
                                        LAST,
                                        List.of(
                                                new ObservedAccountView(
                                                        "account-ref-1",
                                                        "•••• 1234"
                                                )
                                        )
                                )
                        ),
                        summary.firstObservedAt(),
                        summary.lastObservedAt(),
                        summary.totalPayments(),
                        summary.successfulPayments(),
                        summary.failedPayments(),
                        summary.lastPaymentStatus(),
                        summary.lastFailureReasonCode(),
                        summary.projectionUpdatedAt(),
                        summary.projectionVersion(),
                        "event-watermark-secret"
                );

        for (String rendered : List.of(
                summary.toString(),
                detail.toString()
        )) {
            assertFalse(rendered.contains("***3456"));
            assertFalse(rendered.contains("Société ABC"));
            assertFalse(rendered.contains("a***@example.com"));
            assertFalse(rendered.contains("event-watermark-secret"));
        }
    }

    @Test
    void pagesRequireConsistentCursorSemanticsAndSnapshot() {
        ObservedCustomerSummaryView summary = summary();

        ObservedCustomerSearchPage page =
                new ObservedCustomerSearchPage(
                        List.of(summary),
                        1,
                        true,
                        new ObservedCustomerCursor("next"),
                        SNAPSHOT
                );

        assertEquals(1, page.size());
        assertTrue(page.hasMore());
        assertEquals(SNAPSHOT, page.snapshotAt());

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerSearchPage(
                        List.of(summary),
                        0,
                        false,
                        null,
                        SNAPSHOT
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerPaymentPage(
                        List.of(payment()),
                        1,
                        true,
                        null,
                        SNAPSHOT
                )
        );
    }

    @Test
    void paymentViewValidatesSafeMonetaryAndTemporalData() {
        ObservedCustomerPaymentView payment = payment();

        assertEquals(new BigDecimal("15000.00"), payment.amount());
        assertEquals("XAF", payment.currency());
        assertEquals(
                ObservedPaymentStatus.DEBITED,
                payment.status()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerPaymentView(
                        UUID.randomUUID(),
                        "PAY-1",
                        "SIXPAY_BANK",
                        new BigDecimal("-1"),
                        "XAF",
                        ObservedPaymentStatus.DEBITED,
                        null,
                        FIRST,
                        LAST
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerPaymentView(
                        UUID.randomUUID(),
                        "PAY-1",
                        "SIXPAY_BANK",
                        BigDecimal.ONE,
                        "INVALID",
                        ObservedPaymentStatus.DEBITED,
                        null,
                        FIRST,
                        LAST
                )
        );
    }

    private static ObservedCustomerSummaryView summary() {
        return new ObservedCustomerSummaryView(
                customerId(),
                new MaskedIdentifierView("***3456"),
                "Société ABC",
                new MaskedIdentifierView("***1234"),
                new MaskedIdentifierView("a***@example.com"),
                FIRST,
                LAST,
                3,
                1,
                1,
                ObservedPaymentStatus.DEBITED,
                null,
                SNAPSHOT,
                3
        );
    }

    private static ObservedCustomerPaymentView payment() {
        return new ObservedCustomerPaymentView(
                UUID.fromString(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                ),
                "PAY-2026-000123",
                "SIXPAY_BANK",
                new BigDecimal("15000.00"),
                "XAF",
                ObservedPaymentStatus.DEBITED,
                null,
                FIRST,
                LAST
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

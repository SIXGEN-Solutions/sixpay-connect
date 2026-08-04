package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception
        .ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-03T20:00:00Z");

    private static final Instant LAST =
            Instant.parse("2026-08-03T20:05:00Z");

    private static final Instant UPDATED =
            Instant.parse("2026-08-03T20:05:01Z");

    private static final UUID PAYMENT_ONE =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID PAYMENT_TWO =
            UUID.fromString(
                    "22222222-2222-4222-8222-222222222222"
            );

    private static final UUID PAYMENT_THREE =
            UUID.fromString(
                    "33333333-3333-4333-8333-333333333333"
            );

    private static final UUID EVENT_ONE =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final UUID EVENT_TWO =
            UUID.fromString(
                    "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
            );

    private static final UUID EVENT_THREE =
            UUID.fromString(
                    "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
            );

    @Test
    void reconstitutesValidProjectionWithDefensiveCollections() {
        ArrayList<ObservedCustomerInstitution> institutions =
                new ArrayList<>(
                        List.of(institution())
                );

        ArrayList<ObservedPaymentReference> payments =
                new ArrayList<>(
                        validPayments()
                );

        ObservedCustomer customer =
                ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        institutions,
                        payments,
                        Set.of(
                                EVENT_ONE,
                                EVENT_TWO,
                                EVENT_THREE
                        ),
                        FIRST,
                        LAST,
                        3,
                        1,
                        1,
                        ObservedPaymentStatus.POSTING,
                        null,
                        3,
                        ProjectionWatermark.of(
                                EVENT_THREE.toString()
                        ),
                        UPDATED
                );

        institutions.clear();
        payments.clear();

        assertEquals(
                1,
                customer.institutions().size()
        );

        assertEquals(
                3,
                customer.payments().size()
        );

        assertEquals(
                3,
                customer.appliedSourceEventIds().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> customer.institutions().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> customer.payments().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> customer
                        .appliedSourceEventIds()
                        .clear()
        );

        assertEquals(
                3,
                customer.totalPayments()
        );

        assertEquals(
                1,
                customer.successfulPayments()
        );

        assertEquals(
                1,
                customer.failedPayments()
        );

        assertEquals(
                3,
                customer.projectionVersion()
        );

        assertEquals(
                ObservedPaymentStatus.POSTING,
                customer.lastPaymentStatus()
        );
    }

    @Test
    void rejectsInvalidObservationTemporalOrder() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(institution()),
                        List.of(
                                payment(
                                        PAYMENT_ONE,
                                        ObservedPaymentStatus.RECEIVED,
                                        null,
                                        FIRST
                                )
                        ),
                        Set.of(EVENT_ONE),
                        LAST,
                        FIRST,
                        1,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        1,
                        ProjectionWatermark.of(
                                EVENT_ONE.toString()
                        ),
                        UPDATED
                )
        );
    }

    @Test
    void rejectsCountersThatDoNotMatchObservedPayments() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(institution()),
                        List.of(
                                payment(
                                        PAYMENT_ONE,
                                        ObservedPaymentStatus.RECEIVED,
                                        null,
                                        FIRST
                                )
                        ),
                        Set.of(EVENT_ONE),
                        FIRST,
                        LAST,
                        1,
                        1,
                        1,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        1,
                        ProjectionWatermark.of(
                                EVENT_ONE.toString()
                        ),
                        UPDATED
                )
        );
    }

    @Test
    void rejectsTotalThatDoesNotMatchPaymentHistory() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(institution()),
                        List.of(
                                payment(
                                        PAYMENT_ONE,
                                        ObservedPaymentStatus.RECEIVED,
                                        null,
                                        FIRST
                                )
                        ),
                        Set.of(EVENT_ONE),
                        FIRST,
                        LAST,
                        2,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        1,
                        ProjectionWatermark.of(
                                EVENT_ONE.toString()
                        ),
                        UPDATED
                )
        );
    }

    @Test
    void rejectsDuplicatePaymentIdentifiers() {
        ObservedPaymentReference payment =
                payment(
                        PAYMENT_ONE,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST
                );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(institution()),
                        List.of(
                                payment,
                                payment
                        ),
                        Set.of(
                                EVENT_ONE,
                                EVENT_TWO
                        ),
                        FIRST,
                        LAST,
                        2,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        2,
                        ProjectionWatermark.of(
                                EVENT_TWO.toString()
                        ),
                        UPDATED
                )
        );
    }

    @Test
    void rejectsDuplicateInstitutions() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(
                                institution(),
                                institution()
                        ),
                        List.of(
                                payment(
                                        PAYMENT_ONE,
                                        ObservedPaymentStatus.RECEIVED,
                                        null,
                                        FIRST
                                )
                        ),
                        Set.of(EVENT_ONE),
                        FIRST,
                        LAST,
                        1,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        1,
                        ProjectionWatermark.of(
                                EVENT_ONE.toString()
                        ),
                        UPDATED
                )
        );
    }

    @Test
    void toStringDoesNotExposeSensitiveValues() {
        ObservedCustomer customer =
                ObservedCustomer.reconstitute(
                        id(),
                        identity(),
                        List.of(institution()),
                        List.of(
                                payment(
                                        PAYMENT_ONE,
                                        ObservedPaymentStatus.RECEIVED,
                                        null,
                                        FIRST
                                )
                        ),
                        Set.of(EVENT_ONE),
                        FIRST,
                        LAST,
                        1,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        1,
                        ProjectionWatermark.of(
                                "payment-event:secret"
                        ),
                        UPDATED
                );

        String rendered = customer.toString();

        assertFalse(
                rendered.contains("M0123456")
        );

        assertFalse(
                rendered.contains("Société ABC")
        );

        assertFalse(
                rendered.contains(
                        "payment-event:secret"
                )
        );

        assertFalse(
                rendered.contains(
                        "v1:" + "a".repeat(64)
                )
        );

        assertFalse(
                rendered.contains("•••• 1234")
        );

        assertFalse(
                rendered.contains(
                        "a***@example.com"
                )
        );
    }

    private static List<ObservedPaymentReference>
    validPayments() {

        return List.of(
                payment(
                        PAYMENT_ONE,
                        ObservedPaymentStatus.DEBITED,
                        null,
                        FIRST
                ),
                payment(
                        PAYMENT_TWO,
                        ObservedPaymentStatus.REJECTED,
                        "ACCOUNT_NOT_FOUND",
                        LAST.minusSeconds(30)
                ),
                payment(
                        PAYMENT_THREE,
                        ObservedPaymentStatus.POSTING,
                        null,
                        LAST
                )
        );
    }

    private static ObservedCustomerId id() {
        return ObservedCustomerId.of(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                )
        );
    }

    private static ObservedCustomerIdentity identity() {
        return ObservedCustomerIdentity.of(
                "M0123456",
                "Société ABC",
                "***-***-1234",
                "a***@example.com"
        );
    }

    private static ObservedCustomerInstitution institution() {
        return ObservedCustomerInstitution.of(
                "AMPLITUDE",
                FIRST,
                LAST,
                List.of(
                        ObservedAccountReference.of(
                                "v1:" + "a".repeat(64),
                                "•••• 1234"
                        )
                )
        );
    }

    private static ObservedPaymentReference payment(
            UUID paymentId,
            ObservedPaymentStatus status,
            String failureCode,
            Instant updatedAt
    ) {
        return new ObservedPaymentReference(
                paymentId,
                "PAY-" + paymentId,
                "AMPLITUDE",
                new BigDecimal("15000.00"),
                "XAF",
                status,
                failureCode,
                FIRST,
                updatedAt
        );
    }
}
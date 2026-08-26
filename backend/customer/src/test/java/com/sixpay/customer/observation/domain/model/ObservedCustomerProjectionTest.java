package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerProjectionTest {

    private static final UUID CUSTOMER_ID = UUID.fromString(
            "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
    );
    private static final UUID PAYMENT_ONE = UUID.fromString(
            "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
    );
    private static final UUID PAYMENT_TWO = UUID.fromString(
            "54e671e0-5a2a-4af7-bf70-90dfdd555837"
    );

    @Test
    void firstObservationCreatesVersionOneProjection() {
        ObservedCustomer customer = ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        PAYMENT_ONE,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        "2026-08-03T20:00:00Z",
                        "2026-08-03T20:00:00Z"
                )
        );

        assertEquals(1, customer.totalPayments());
        assertEquals(0, customer.successfulPayments());
        assertEquals(0, customer.failedPayments());
        assertEquals(1, customer.projectionVersion());
        assertEquals(
                ObservedPaymentStatus.RECEIVED,
                customer.lastPaymentStatus()
        );
    }

    @Test
    void newPaymentIncrementsTotalsAndOutcomeCounter() {
        ObservedCustomer customer = first();

        ObservationApplicationResult result =
                customer.observePayment(
                        observation(
                                UUID.fromString(
                                        "22222222-2222-4222-8222-222222222222"
                                ),
                                PAYMENT_TWO,
                                ObservedPaymentStatus.REJECTED,
                                "ACCOUNT_NOT_FOUND",
                                "2026-08-03T20:05:00Z",
                                "2026-08-03T20:05:00Z"
                        )
                );

        assertEquals(
                ObservationApplicationResult.APPLIED_NEW_PAYMENT,
                result
        );
        assertEquals(2, customer.totalPayments());
        assertEquals(0, customer.successfulPayments());
        assertEquals(1, customer.failedPayments());
        assertEquals(
                ObservedPaymentStatus.REJECTED,
                customer.lastPaymentStatus()
        );
        assertEquals(
                "ACCOUNT_NOT_FOUND",
                customer.lastFailureReasonCode().orElseThrow()
        );
        assertEquals(2, customer.projectionVersion());
    }

    @Test
    void paymentUpdateAdjustsCountersWithoutIncreasingTotal() {
        ObservedCustomer customer = first();

        ObservationApplicationResult result =
                customer.observePayment(
                        observation(
                                UUID.fromString(
                                        "33333333-3333-4333-8333-333333333333"
                                ),
                                PAYMENT_ONE,
                                ObservedPaymentStatus.DEBITED,
                                null,
                                "2026-08-03T20:05:00Z",
                                "2026-08-03T20:05:00Z"
                        )
                );

        assertEquals(
                ObservationApplicationResult.APPLIED_PAYMENT_UPDATE,
                result
        );
        assertEquals(1, customer.totalPayments());
        assertEquals(1, customer.successfulPayments());
        assertEquals(0, customer.failedPayments());
        assertEquals(
                ObservedPaymentStatus.DEBITED,
                customer.lastPaymentStatus()
        );
    }

    @Test
    void identicalSourceEventIsNoOp() {
        UUID sourceEventId = UUID.fromString(
                "44444444-4444-4444-8444-444444444444"
        );
        ObservedCustomerObservation observation = observation(
                sourceEventId,
                PAYMENT_TWO,
                ObservedPaymentStatus.REJECTED,
                "ACCOUNT_NOT_FOUND",
                "2026-08-03T20:05:00Z",
                "2026-08-03T20:05:00Z"
        );

        ObservedCustomer customer = first();
        customer.observePayment(observation);

        long version = customer.projectionVersion();
        long total = customer.totalPayments();

        assertEquals(
                ObservationApplicationResult.REPLAYED,
                customer.observePayment(observation)
        );
        assertEquals(version, customer.projectionVersion());
        assertEquals(total, customer.totalPayments());
    }

    @Test
    void stalePaymentEventDoesNotReplaceLatestStatus() {
        ObservedCustomer customer = first();

        customer.observePayment(
                observation(
                        UUID.fromString(
                                "55555555-5555-4555-8555-555555555555"
                        ),
                        PAYMENT_ONE,
                        ObservedPaymentStatus.DEBITED,
                        null,
                        "2026-08-03T20:10:00Z",
                        "2026-08-03T20:10:00Z"
                )
        );

        ObservationApplicationResult result =
                customer.observePayment(
                        observation(
                                UUID.fromString(
                                        "66666666-6666-4666-8666-666666666666"
                                ),
                                PAYMENT_ONE,
                                ObservedPaymentStatus.REJECTED,
                                "ACCOUNT_NOT_FOUND",
                                "2026-08-03T20:04:00Z",
                                "2026-08-03T20:11:00Z"
                        )
                );

        assertEquals(
                ObservationApplicationResult.APPLIED_STALE_HISTORY,
                result
        );
        assertEquals(
                ObservedPaymentStatus.DEBITED,
                customer.lastPaymentStatus()
        );
        assertEquals(1, customer.successfulPayments());
        assertEquals(0, customer.failedPayments());
    }

    @Test
    void conflictingIdentityIsRejected() {
        ObservedCustomer customer = first();

        ObservedCustomerObservation conflicting =
                new ObservedCustomerObservation(
                        UUID.fromString(
                                "77777777-7777-4777-8777-777777777777"
                        ),
                        ObservedCustomerIdentity.of(
                                "M0123456",
                                "Different Legal Name",
                                null,
                                null
                        ),
                        institution(
                                "2026-08-03T20:05:00Z"
                        ),
                        payment(
                                PAYMENT_TWO,
                                ObservedPaymentStatus.RECEIVED,
                                null,
                                "2026-08-03T20:05:00Z"
                        ),
                        ProjectionWatermark.of(
                                "77777777-7777-4777-8777-777777777777"
                        ),
                        Instant.parse(
                                "2026-08-03T20:05:00Z"
                        ),
                        Instant.parse(
                                "2026-08-03T20:05:01Z"
                        )
                );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> customer.observePayment(conflicting)
        );
    }

    @Test
    void reconstitutionValidatesPaymentsCountersAndAppliedEvents() {
        ObservedPaymentReference payment = payment(
                PAYMENT_ONE,
                ObservedPaymentStatus.DEBITED,
                null,
                "2026-08-03T20:00:00Z"
        );

        ObservedCustomer customer = ObservedCustomer.reconstitute(
                ObservedCustomerId.of(CUSTOMER_ID),
                identity(),
                List.of(institution("2026-08-03T20:00:00Z")),
                List.of(payment),
                Set.of(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        )
                ),
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:00:00Z"),
                1,
                1,
                0,
                ObservedPaymentStatus.DEBITED,
                null,
                3,
                ProjectionWatermark.of("event:3"),
                Instant.parse("2026-08-03T20:00:01Z")
        );

        assertEquals(1, customer.payments().size());
        assertEquals(1, customer.appliedSourceEventIds().size());
        assertEquals(3, customer.projectionVersion());
    }

    private static ObservedCustomer first() {
        return ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        PAYMENT_ONE,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        "2026-08-03T20:00:00Z",
                        "2026-08-03T20:00:00Z"
                )
        );
    }

    private static ObservedCustomerObservation observation(
            UUID sourceEventId,
            UUID paymentId,
            ObservedPaymentStatus status,
            String failureCode,
            String paymentUpdatedAt,
            String observedAt
    ) {
        return new ObservedCustomerObservation(
                sourceEventId,
                identity(),
                institution(observedAt),
                payment(
                        paymentId,
                        status,
                        failureCode,
                        paymentUpdatedAt
                ),
                ProjectionWatermark.of(
                        sourceEventId.toString()
                ),
                Instant.parse(observedAt),
                Instant.parse(observedAt).plusSeconds(1)
        );
    }

    private static ObservedCustomerIdentity identity() {
        return ObservedCustomerIdentity.of(
                "M0123456",
                "Société ABC SARL",
                "***-***-1234",
                "a***@example.com"
        );
    }

    private static ObservedCustomerInstitution institution(
            String observedAt
    ) {
        Instant instant = Instant.parse(observedAt);

        return ObservedCustomerInstitution.of(
                "AMPLITUDE",
                instant,
                instant,
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
            String updatedAt
    ) {
        return new ObservedPaymentReference(
                paymentId,
                "PAY-" + paymentId,
                "AMPLITUDE",
                new BigDecimal("15000.00"),
                "XAF",
                status,
                failureCode,
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse(updatedAt)
        );
    }
}

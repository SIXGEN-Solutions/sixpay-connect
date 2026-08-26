package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerLot457DomainTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-03T20:00:00Z");

    private static final UUID CUSTOMER_ID =
            UUID.fromString(
                    "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
            );

    @Test
    void firstAndSubsequentObservationsKeepCanonicalCountersAndTimes() {
        ObservedCustomer customer = ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        event("11111111"),
                        PAYMENT_ID,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST
                )
        );

        assertEquals(1, customer.totalPayments());
        assertEquals(FIRST, customer.firstObservedAt());
        assertEquals(FIRST, customer.lastObservedAt());
        assertEquals(1, customer.projectionVersion());

        Instant later = FIRST.plusSeconds(60);

        customer.observePayment(
                observation(
                        event("22222222"),
                        PAYMENT_ID,
                        ObservedPaymentStatus.DEBITED,
                        null,
                        later,
                        later
                )
        );

        assertEquals(1, customer.totalPayments());
        assertEquals(1, customer.successfulPayments());
        assertEquals(0, customer.failedPayments());
        assertEquals(FIRST, customer.firstObservedAt());
        assertEquals(later, customer.lastObservedAt());
        assertEquals(
                ObservedPaymentStatus.DEBITED,
                customer.lastPaymentStatus()
        );
        assertEquals(2, customer.projectionVersion());
    }

    @Test
    void replayIsNoOpAndStaleEventCannotReplaceLatestStatus() {
        ObservedCustomer customer = ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        event("11111111"),
                        PAYMENT_ID,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST
                )
        );

        UUID latestEvent = event("22222222");
        Instant latest = FIRST.plusSeconds(120);

        ObservedCustomerObservation current = observation(
                latestEvent,
                PAYMENT_ID,
                ObservedPaymentStatus.DEBITED,
                null,
                latest,
                latest
        );

        customer.observePayment(current);

        long version = customer.projectionVersion();

        assertEquals(
                ObservationApplicationResult.REPLAYED,
                customer.observePayment(current)
        );
        assertEquals(version, customer.projectionVersion());

        assertEquals(
                ObservationApplicationResult.APPLIED_STALE_HISTORY,
                customer.observePayment(
                        observation(
                                event("33333333"),
                                PAYMENT_ID,
                                ObservedPaymentStatus.REJECTED,
                                "ACCOUNT_NOT_FOUND",
                                FIRST.plusSeconds(30),
                                latest.plusSeconds(1)
                        )
                )
        );

        assertEquals(
                ObservedPaymentStatus.DEBITED,
                customer.lastPaymentStatus()
        );
        assertEquals(1, customer.successfulPayments());
        assertEquals(0, customer.failedPayments());
    }

    @Test
    void contradictoryIdentityAndRawAccountAreRejected() {
        ObservedCustomer customer = ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        event("11111111"),
                        PAYMENT_ID,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST
                )
        );

        ObservedCustomerObservation conflict =
                new ObservedCustomerObservation(
                        event("44444444"),
                        ObservedCustomerIdentity.of(
                                "M0123456",
                                "Another Legal Entity",
                                null,
                                null
                        ),
                        institution(FIRST.plusSeconds(10)),
                        payment(
                                PAYMENT_ID,
                                ObservedPaymentStatus.RECEIVED,
                                null,
                                FIRST.plusSeconds(10)
                        ),
                        ProjectionWatermark.of("event:conflict"),
                        FIRST.plusSeconds(10),
                        FIRST.plusSeconds(11)
                );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> customer.observePayment(conflict)
        );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedAccountReference.of(
                        "v1:" + "a".repeat(64),
                        "10005-00001-12345678901-12"
                )
        );
    }

    @Test
    void institutionsAreImmutableAndTimeIsAlwaysSupplied() {
        ObservedCustomer customer = ObservedCustomer.observeFirst(
                ObservedCustomerId.of(CUSTOMER_ID),
                observation(
                        event("11111111"),
                        PAYMENT_ID,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        FIRST,
                        FIRST
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> customer.institutions().clear()
        );

        String aggregateSource = readSource(
                "src/main/java/com/sixpay/customer/observation/"
                        + "domain/model/ObservedCustomer.java"
        );

        assertFalse(aggregateSource.contains("Instant.now("));
        assertFalse(aggregateSource.contains(
                "System.currentTimeMillis("
        ));
    }

    private static ObservedCustomerObservation observation(
            UUID sourceEventId,
            UUID paymentId,
            ObservedPaymentStatus status,
            String failureCode,
            Instant paymentUpdatedAt,
            Instant observedAt
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
                observedAt,
                observedAt.plusSeconds(1)
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
            Instant observedAt
    ) {
        return ObservedCustomerInstitution.of(
                "AMPLITUDE",
                observedAt,
                observedAt,
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

    private static UUID event(String prefix) {
        return UUID.fromString(
                prefix + "-1111-4111-8111-111111111111"
        );
    }

    private static String readSource(String path) {
        try {
            return java.nio.file.Files.readString(
                    java.nio.file.Path.of(path)
            );
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

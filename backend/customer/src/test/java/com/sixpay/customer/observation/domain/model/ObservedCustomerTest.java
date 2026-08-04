package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-03T20:00:00Z");
    private static final Instant LAST =
            Instant.parse("2026-08-03T20:05:00Z");
    private static final Instant UPDATED =
            Instant.parse("2026-08-03T20:05:01Z");

    @Test
    void reconstitutesValidProjectionWithDefensiveCollections() {
        ArrayList<ObservedCustomerInstitution> institutions =
                new ArrayList<>(List.of(institution()));

        ObservedCustomer customer = ObservedCustomer.reconstitute(
                id(),
                identity(),
                institutions,
                FIRST,
                LAST,
                3,
                1,
                1,
                ObservedPaymentStatus.POSTING,
                null,
                3,
                ProjectionWatermark.of("payment-event:3"),
                UPDATED
        );

        institutions.clear();

        assertEquals(1, customer.institutions().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> customer.institutions().clear()
        );
        assertEquals(3, customer.projectionVersion());
    }

    @Test
    void rejectsInvalidCountersAndTemporalOrder() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(), identity(), List.of(institution()),
                        LAST, FIRST, 1, 0, 0,
                        ObservedPaymentStatus.RECEIVED,
                        null, 1,
                        ProjectionWatermark.of("event:1"),
                        UPDATED
                )
        );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomer.reconstitute(
                        id(), identity(), List.of(institution()),
                        FIRST, LAST, 1, 1, 1,
                        ObservedPaymentStatus.REJECTED,
                        "ACCOUNT_NOT_FOUND", 1,
                        ProjectionWatermark.of("event:1"),
                        UPDATED
                )
        );
    }

    @Test
    void toStringDoesNotExposeSensitiveValues() {
        ObservedCustomer customer = ObservedCustomer.reconstitute(
                id(), identity(), List.of(institution()),
                FIRST, LAST, 1, 0, 0,
                ObservedPaymentStatus.RECEIVED,
                null, 1,
                ProjectionWatermark.of("payment-event:secret"),
                UPDATED
        );

        String rendered = customer.toString();
        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Société ABC"));
        assertFalse(rendered.contains("payment-event:secret"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
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
}

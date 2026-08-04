package com.sixpay.customer.observation.application.port.input;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserveCustomerResultTest {

    @Test
    void exposesAppliedAndReplayDisposition() {
        ObserveCustomerResult applied = result(
                ObserveCustomerResult.Disposition.APPLIED
        );
        ObserveCustomerResult replayed = result(
                ObserveCustomerResult.Disposition.REPLAYED
        );

        assertTrue(applied.applied());
        assertFalse(applied.replayed());
        assertTrue(replayed.replayed());
        assertFalse(replayed.applied());
    }

    @Test
    void rejectsInvalidProjectionVersion() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> new ObserveCustomerResult(
                        ObservedCustomerId.of(
                                UUID.fromString(
                                        "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                                )
                        ),
                        UUID.fromString(
                                "54e671e0-5a2a-4af7-bf70-90dfdd555837"
                        ),
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        ),
                        ObserveCustomerResult.Disposition.APPLIED,
                        0,
                        Instant.parse("2026-08-03T20:05:02Z")
                )
        );
    }

    private static ObserveCustomerResult result(
            ObserveCustomerResult.Disposition disposition
    ) {
        return new ObserveCustomerResult(
                ObservedCustomerId.of(
                        UUID.fromString(
                                "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                        )
                ),
                UUID.fromString(
                        "54e671e0-5a2a-4af7-bf70-90dfdd555837"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                disposition,
                3,
                Instant.parse("2026-08-03T20:05:02Z")
        );
    }
}

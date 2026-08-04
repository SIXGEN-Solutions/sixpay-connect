package com.sixpay.customer.observation.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerArchitectureTest {

    private static final Path AGGREGATE = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "domain/model/ObservedCustomer.java"
    );

    @Test
    void aggregateUsesNamedOperationsWithoutGenericSetters()
            throws Exception {

        String source = Files.readString(AGGREGATE);

        for (String required : List.of(
                "ObservedCustomer observeFirst(",
                "ObservationApplicationResult observePayment(",
                "ObservedCustomer reconstitute("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing named operation: " + required
            );
        }

        for (String forbidden : List.of(
                "setStatus(",
                "setLastObservedAt(",
                "incrementTotalPayments(",
                "transitionTo(",
                "Instant.now(",
                "UUID.randomUUID(",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.verification.",
                "import org.springframework.",
                "import jakarta.persistence."
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden aggregate concept: "
                            + forbidden
            );
        }
    }

    @Test
    void replayAndStaleEventRulesAreExplicit()
            throws Exception {

        String source = Files.readString(AGGREGATE);

        assertTrue(source.contains(
                "appliedSourceEventIds.contains("
        ));
        assertTrue(source.contains(
                "ObservationApplicationResult.REPLAYED"
        ));
        assertTrue(source.contains(
                ".isBefore(current.updatedAt())"
        ));
        assertTrue(source.contains(
                "APPLIED_STALE_HISTORY"
        ));
        assertTrue(source.contains(
                "ObservedCustomerIdentityPolicy.requireCompatible("
        ));
    }
}

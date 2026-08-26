package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryContractTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void openApiAndSpringMvcExposeSameObservedCustomerOperations()
            throws Exception {

        String contract = Files.readString(
                ROOT.resolve(
                        "documentation/contracts/internal/"
                                + "observed-customer-query-api-v1.yaml"
                )
        );
        String controller = Files.readString(
                ROOT.resolve(
                        "backend/customer/src/main/java/"
                                + "com/sixpay/customer/observation/"
                                + "api/controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        assertTrue(contract.contains(
                "/internal/api/v1/observed-customers:"
        ));
        assertTrue(contract.contains(
                "/internal/api/v1/observed-customers/"
                        + "{observedCustomerId}:"
        ));
        assertTrue(contract.contains(
                "/internal/api/v1/observed-customers/"
                        + "{observedCustomerId}/payments:"
        ));

        assertTrue(controller.contains(
                "/internal/api/v1/observed-customers"
        ));
        assertTrue(controller.contains(
                "SCOPE_observed-customer.read"
        ));

        assertFalse(
                controller.contains("Instant snapshotAt"),
                "snapshotAt must remain server/cursor owned"
        );
    }
}

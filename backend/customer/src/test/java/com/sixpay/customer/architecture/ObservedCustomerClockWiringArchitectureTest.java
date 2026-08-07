package com.sixpay.customer.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerClockWiringArchitectureTest {

    private static final Path ROOT =
            Path.of(
                    "src/main/java/com/sixpay/customer/"
                            + "observation"
            );

    @Test
    void observedCustomerClockIsNamedAndModuleScoped()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "configuration/"
                                + "ObservedCustomerObservabilityConfiguration.java"
                )
        );

        assertTrue(
                source.contains(
                        "OBSERVED_CUSTOMER_CLOCK"
                )
        );

        assertTrue(
                source.contains(
                        "name = OBSERVED_CUSTOMER_CLOCK"
                )
        );
    }

    @Test
    void queryConfigurationUsesObservedCustomerClock()
            throws Exception {
        assertUsesObservedCustomerClock(
                ROOT.resolve(
                        "api/configuration/"
                                + "ObservedCustomerQueryApiConfiguration.java"
                )
        );
    }

    @Test
    void queryControllerUsesObservedCustomerClock()
            throws Exception {
        assertUsesObservedCustomerClock(
                ROOT.resolve(
                        "api/controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );
    }

    private static void assertUsesObservedCustomerClock(
            Path path
    ) throws Exception {
        String source = Files.readString(path);

        assertTrue(
                source.contains("@Qualifier("),
                () -> path + " must qualify its Clock"
        );

        assertTrue(
                source.contains(
                        ".OBSERVED_CUSTOMER_CLOCK"
                ),
                () -> path
                        + " must use observedCustomerClock"
        );
    }
}

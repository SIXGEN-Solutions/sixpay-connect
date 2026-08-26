package com.sixpay.customer.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerPersistenceClockWiringArchitectureTest {

    @Test
    void persistenceConfigurationUsesObservedCustomerClock()
            throws Exception {

        String source = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/customer/"
                                + "observation/configuration/"
                                + "ObservedCustomerPersistenceConfiguration.java"
                )
        );

        assertTrue(
                source.contains(
                        "@Qualifier("
                )
        );

        assertTrue(
                source.contains(
                        ".OBSERVED_CUSTOMER_CLOCK"
                )
        );

        assertTrue(
                source.contains(
                        "ObjectProvider<Clock> clockProvider"
                )
        );
    }
}

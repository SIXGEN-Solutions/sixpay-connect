package com.sixpay.customer.observation.configuration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerQueryPropertiesTest {

    @Test
    void enabledQueryRequiresDistinctValidCursorKey() {
        String encoded = Base64.getEncoder()
                .encodeToString(
                        "0123456789abcdef0123456789abcdef"
                                .getBytes(StandardCharsets.UTF_8)
                );

        ObservedCustomerQueryProperties properties =
                new ObservedCustomerQueryProperties(
                        true,
                        encoded
                );

        assertEquals(
                32,
                properties.decodedCursorKey().length
        );

        assertThrows(
                NullPointerException.class,
                () -> new ObservedCustomerQueryProperties(
                        true,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerQueryProperties(
                        true,
                        Base64.getEncoder()
                                .encodeToString(
                                        "short".getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                )
                )
        );
    }
}

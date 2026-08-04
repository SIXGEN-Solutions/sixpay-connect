package com.sixpay.customer.observation.infrastructure.persistence.protection;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesGcmObservedCustomerDataProtectorTest {

    private static final String KEY =
            Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes(StandardCharsets.UTF_8)
            );

    private final AesGcmObservedCustomerDataProtector protector =
            new AesGcmObservedCustomerDataProtector(KEY);

    @Test
    void protectionIsReversibleAndRandomized() {
        String first = protector.protect("M0123456");
        String second = protector.protect("M0123456");

        assertTrue(first.startsWith("v1:"));
        assertNotEquals(first, second);
        assertEquals(
                "M0123456",
                protector.reveal(first)
        );
        assertEquals(
                "M0123456",
                protector.reveal(second)
        );
    }

    @Test
    void lookupHashIsDeterministic() {
        assertEquals(
                protector.searchHash("M0123456"),
                protector.searchHash("M0123456")
        );
        assertNotEquals(
                protector.searchHash("M0123456"),
                protector.searchHash("M9999999")
        );
    }
}

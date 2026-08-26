package com.sixpay.customer.verification.application.port.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankingAccountAccessReferenceTest {

    @Test
    void acceptsAndRedactsOpaqueReference() {
        BankingAccountAccessReference reference =
                BankingAccountAccessReference.of(" AMP-ACC-000123 ");

        assertEquals("AMP-ACC-000123", reference.value());
        assertEquals("[PROTECTED_BANKING_REFERENCE]", reference.toString());
    }

    @Test
    void rejectsInvalidReferences() {
        assertThrows(
                NullPointerException.class,
                () -> BankingAccountAccessReference.of(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BankingAccountAccessReference.of(" ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BankingAccountAccessReference.of("A".repeat(257))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BankingAccountAccessReference.of("AMP\nACCOUNT")
        );
    }
}

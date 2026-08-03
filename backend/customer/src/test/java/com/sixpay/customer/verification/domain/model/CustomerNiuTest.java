package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerNiuTest {

    @Test
    void normalizesWhitespaceAndCase() {
        CustomerNiu niu = CustomerNiu.of("  m 0123 456  ");

        assertEquals("M0123456", niu.value());
    }

    @Test
    void doesNotExposeTheRawValueThroughToString() {
        CustomerNiu niu = CustomerNiu.of("M0123456");

        assertEquals("[PROTECTED_NIU]", niu.toString());
    }

    @Test
    void rejectsBlankOversizedAndInvalidValues() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerNiu.of("   ")
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerNiu.of("A".repeat(65))
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerNiu.of("NIU#123")
        );
    }
}

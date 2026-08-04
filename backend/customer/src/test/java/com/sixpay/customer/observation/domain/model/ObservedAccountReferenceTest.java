package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservedAccountReferenceTest {

    @Test
    void acceptsOnlyProtectedFingerprintAndMaskedDisplay() {
        var reference = ObservedAccountReference.of(
                "v1:" + "a".repeat(64),
                "•••• 1234"
        );

        String rendered = reference.toString();
        assertFalse(rendered.contains("v1:"));
        assertFalse(rendered.contains("1234"));
    }

    @Test
    void rejectsRawAccountDisplayOrInvalidFingerprint() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedAccountReference.of(
                        "v1:" + "a".repeat(64),
                        "10005-00001-12345678901-12"
                )
        );
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedAccountReference.of(
                        "10005-00001-12345678901-12",
                        "•••• 1234"
                )
        );
    }
}

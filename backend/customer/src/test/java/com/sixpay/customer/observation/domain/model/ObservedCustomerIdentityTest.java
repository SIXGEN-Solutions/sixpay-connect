package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerIdentityTest {

    @Test
    void normalizesAndProtectsIdentity() {
        var identity = ObservedCustomerIdentity.of(
                " m 0123456 ",
                "  Société   ABC SARL ",
                "***-***-1234",
                "a***@example.com"
        );

        assertEquals("M0123456", identity.normalizedNiu());
        assertEquals("Société ABC SARL", identity.legalName());

        String rendered = identity.toString();
        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Société ABC SARL"));
        assertFalse(rendered.contains("example.com"));
    }

    @Test
    void rejectsUnmaskedContacts() {
        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomerIdentity.of(
                        "M0123456",
                        "Société ABC",
                        "6135551234",
                        null
                )
        );
    }
}

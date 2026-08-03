package com.sixpay.customer.verification.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationSubjectTest {

    @Test
    void requiresAnIdentity() {
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationSubject(null)
        );
    }

    @Test
    void doesNotExposeIdentityDataThroughToString() {
        CustomerVerificationSubject subject =
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of("M0123456"),
                                "Ada Lovelace"
                        )
                );

        String rendered = subject.toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
    }
}

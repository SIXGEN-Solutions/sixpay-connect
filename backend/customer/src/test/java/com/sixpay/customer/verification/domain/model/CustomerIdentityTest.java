package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerIdentityTest {

    @Test
    void normalizesLegalNameWhitespace() {
        CustomerIdentity identity = CustomerIdentity.of(
                CustomerNiu.of("M0123456"),
                "  Ada    Lovelace  "
        );

        assertEquals("Ada Lovelace", identity.legalName());
    }

    @Test
    void doesNotExposeIdentityDataThroughToString() {
        CustomerIdentity identity = CustomerIdentity.of(
                CustomerNiu.of("M0123456"),
                "Ada Lovelace"
        );

        String rendered = identity.toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
    }

    @Test
    void rejectsMissingShortAndOversizedNames() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerIdentity.of(
                        CustomerNiu.of("M0123456"),
                        " "
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerIdentity.of(
                        CustomerNiu.of("M0123456"),
                        "A"
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerIdentity.of(
                        CustomerNiu.of("M0123456"),
                        "A".repeat(201)
                )
        );
    }
}

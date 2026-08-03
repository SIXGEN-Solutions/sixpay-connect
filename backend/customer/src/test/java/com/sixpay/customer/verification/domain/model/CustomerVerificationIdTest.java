package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationIdTest {

    @Test
    void acceptsAnExternallyGeneratedUuid() {
        UUID value = UUID.fromString(
                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
        );

        CustomerVerificationId id =
                new CustomerVerificationId(value);

        assertEquals(value, id.value());
        assertEquals(value.toString(), id.toString());
    }

    @Test
    void parsesAnExistingUuidWithoutGeneratingOne() {
        String value = "7ed75090-8af7-4dfa-9b62-8e4dca73501a";

        assertEquals(
                UUID.fromString(value),
                CustomerVerificationId.from(value).value()
        );
    }

    @Test
    void rejectsNullNilAndMalformedIdentifiers() {
        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationId(null)
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationId(
                        new UUID(0L, 0L)
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerVerificationId.from("not-a-uuid")
        );
    }
}

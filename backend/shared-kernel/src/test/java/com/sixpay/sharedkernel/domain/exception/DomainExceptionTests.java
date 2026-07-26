package com.sixpay.sharedkernel.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainExceptionTest {

    @Test
    void shouldCreateDomainException() {
        DomainException exception = new DomainException(
                "PAYMENT_REJECTED",
                "Payment was rejected"
        );

        assertEquals("PAYMENT_REJECTED", exception.code());
        assertEquals("Payment was rejected", exception.getMessage());
    }

    @Test
    void shouldRejectBlankCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DomainException(
                        " ",
                        "Payment was rejected"
                )
        );
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DomainException(
                        "PAYMENT_REJECTED",
                        " "
                )
        );
    }
}
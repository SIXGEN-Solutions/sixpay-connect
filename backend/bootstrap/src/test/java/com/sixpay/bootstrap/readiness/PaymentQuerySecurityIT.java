package com.sixpay.bootstrap.readiness;

import com.sixpay.payment.application.security.PaymentAuthority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentQuerySecurityIT {

    @Test
    void paymentReadAuthorityMatchesPublishedScope() {
        assertEquals(
                "SCOPE_payment.read",
                PaymentAuthority.READ.authority()
        );
    }
}

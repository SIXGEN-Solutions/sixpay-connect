package com.sixpay.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentStateDocumentV3Test {

    @Test
    void currentPaymentStateSchemaIsVersionThree() {
        assertEquals(
                3,
                PaymentStateDocument.CURRENT_SCHEMA_VERSION
        );
    }
}

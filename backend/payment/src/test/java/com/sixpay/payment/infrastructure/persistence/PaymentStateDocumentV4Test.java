package com.sixpay.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentStateDocumentV4Test {

    @Test
    void currentPaymentStateSchemaIsVersionFour() {
        assertEquals(
                4,
                PaymentStateDocument.CURRENT_SCHEMA_VERSION
        );
    }
}

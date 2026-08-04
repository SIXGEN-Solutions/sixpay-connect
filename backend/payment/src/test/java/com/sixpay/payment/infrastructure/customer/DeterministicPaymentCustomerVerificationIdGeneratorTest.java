package com.sixpay.payment.infrastructure.customer;

import com.sixpay.payment.domain.model.PaymentId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicPaymentCustomerVerificationIdGeneratorTest {

    private final DeterministicPaymentCustomerVerificationIdGenerator generator =
            new DeterministicPaymentCustomerVerificationIdGenerator();

    @Test
    void samePaymentAlwaysProducesSameVerificationId() {
        PaymentId paymentId = new PaymentId(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                )
        );

        assertEquals(
                generator.forPayment(paymentId),
                generator.forPayment(paymentId)
        );
    }

    @Test
    void differentPaymentsProduceDifferentVerificationIds() {
        assertNotEquals(
                generator.forPayment(
                        new PaymentId(
                                UUID.fromString(
                                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                                )
                        )
                ),
                generator.forPayment(
                        new PaymentId(
                                UUID.fromString(
                                        "54e671e0-5a2a-4af7-bf70-90dfdd555837"
                                )
                        )
                )
        );
    }
}

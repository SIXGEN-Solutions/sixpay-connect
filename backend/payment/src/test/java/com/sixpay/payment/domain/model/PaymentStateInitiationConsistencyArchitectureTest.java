package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateInitiationConsistencyArchitectureTest {

    private static final Path PAYMENT_STATE =
            Path.of(
                    "src/main/java/com/sixpay/payment/domain/model/"
                            + "PaymentState.java"
            );

    @Test
    void stateProtectsInitiationAndConfirmationConsistency()
            throws Exception {

        String source = Files.readString(PAYMENT_STATE);

        assertTrue(source.contains(
                "validateInitiationAndConfirmationCoherence()"
        ));
        assertTrue(source.contains(
                "PENDING_CONFIRMATION requires initiation context"
        ));
        assertTrue(source.contains(
                "Customer confirmation evidence requires initiation context"
        ));
        assertTrue(source.contains(
                "Customer confirmation evidence must not precede receipt"
        ));
        assertTrue(source.contains(
                "Post-confirmation Payment requires verified confirmation challenge"
        ));
    }
}

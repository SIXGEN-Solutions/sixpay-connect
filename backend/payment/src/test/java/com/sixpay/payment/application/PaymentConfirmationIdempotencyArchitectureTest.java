package com.sixpay.payment.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentConfirmationIdempotencyArchitectureTest {

    private static final Path MAIN =
            Path.of("src/main/java/com/sixpay/payment");

    @Test
    void confirmationServiceRoutesMutationsThroughIdempotencyPort()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentConfirmationService.java"
                )
        );

        assertTrue(source.contains(
                "PaymentConfirmationIdempotencyPort"
        ));
        assertTrue(source.contains(
                "idempotencyPort.executeCreate("
        ));
        assertTrue(source.contains(
                "idempotencyPort.executeVerify("
        ));
        assertTrue(source.contains(
                "idempotencyPort.executeReplace("
        ));

        assertFalse(source.contains(
                "PaymentAtomicPersistencePort"
        ));
    }

    @Test
    void verifyFingerprintReusesApprovedHmacComponent()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "infrastructure/idempotency/"
                                + "PaymentOtpIdempotencyFingerprintSet.java"
                )
        );

        assertTrue(source.contains(
                "PaymentOtpIdempotencyFingerprint"
        ));
        assertFalse(source.contains("MessageDigest"));
    }

    @Test
    void recoveryMethodCannotCallOriginalMutationSupplier()
            throws Exception {

        String source = Files.readString(
                MAIN.resolve(
                        "infrastructure/idempotency/"
                                + "PaymentConfirmationIdempotencyAdapter.java"
                )
        );

        int start = source.indexOf(
                "private PaymentConfirmationBankResult recover("
        );
        int end = source.indexOf(
                "private void complete(",
                start
        );

        String recoveryMethod = source.substring(start, end);

        assertTrue(recoveryMethod.contains("recovery.get()"));
        assertFalse(recoveryMethod.contains("newRequest.get()"));
    }
}

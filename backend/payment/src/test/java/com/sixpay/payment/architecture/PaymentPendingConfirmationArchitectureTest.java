package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPendingConfirmationArchitectureTest {

    private static final Path DOMAIN =
            Path.of("src/main/java/com/sixpay/payment/domain");

    @Test
    void pendingConfirmationIsAFirstClassLifecycleState()
            throws Exception {
        String status = Files.readString(
                DOMAIN.resolve("model/PaymentStatus.java")
        );
        String payment = Files.readString(
                DOMAIN.resolve("model/Payment.java")
        );
        String state = Files.readString(
                DOMAIN.resolve("model/PaymentState.java")
        );

        assertTrue(status.contains(
                "PENDING_CONFIRMATION(false)"
        ));
        assertTrue(payment.contains(
                "startBankingVerification"
        ));
        assertTrue(payment.contains(
                "recordBankingVerification"
        ));
        assertTrue(payment.contains(
                "recordCustomerConfirmation"
        ));
        assertTrue(state.contains(
                "PENDING_CONFIRMATION"
        ));
    }
}

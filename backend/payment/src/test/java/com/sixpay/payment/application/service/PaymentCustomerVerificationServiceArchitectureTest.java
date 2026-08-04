package com.sixpay.payment.application.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerVerificationServiceArchitectureTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/sixpay/payment/application/service/"
                    + "PaymentCustomerVerificationService.java"
    );

    @Test
    void serviceUsesPaymentOwnedPortsAndCanonicalMutationCoordinator()
            throws Exception {

        String source = Files.readString(SERVICE);

        for (String required : List.of(
                "PaymentMutationCoordinator",
                "CustomerVerificationPort",
                "PaymentCustomerVerificationRequestFactory",
                "CustomerVerificationEvidenceMapper",
                "CustomerVerificationFailureMapper",
                "PaymentCustomerVerificationIdGenerator",
                "coordinator.mutate(",
                "payment.recordBankingVerification("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing workflow element: " + required
            );
        }

        for (String forbidden : List.of(
                "import com.sixpay.customer.",
                "AmplitudeCustomerVerification",
                "RestClient",
                "HttpClient",
                "UUID.randomUUID(",
                "Instant.now(",
                "PaymentAtomicPersistencePort"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden workflow dependency: " + forbidden
            );
        }
    }
}

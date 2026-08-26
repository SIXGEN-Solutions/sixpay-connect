package com.sixpay.payment.application.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerVerificationReplayArchitectureTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/sixpay/payment/application/service/"
                    + "PaymentCustomerVerificationService.java"
    );

    @Test
    void technicalFailureDoesNotCreateBusinessRejectionOrEvidence()
            throws Exception {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains(
                "catch (CustomerVerificationTechnicalException"
        ));
        assertTrue(source.contains(
                "PaymentCustomerVerificationRetryableException"
        ));

        for (String forbidden : List.of(
                "BankingVerificationOutcome.REJECTED",
                "CustomerVerificationResponse.Outcome.REJECTED",
                "recordBankingVerification(" +
                        "\n                            null",
                "UUID.randomUUID(",
                "Instant.now("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden technical-failure handling: "
                            + forbidden
            );
        }
    }

    @Test
    void replayUsesStableIdentifiersAndCanonicalCustomerTime()
            throws Exception {
        String source = Files.readString(SERVICE);

        assertTrue(source.contains(
                "idGenerator.forPayment(paymentId)"
        ));
        assertTrue(source.contains(
                "response.completedAt()"
        ));
        assertTrue(source.contains(
                "requestIdentity()"
        ));
        assertTrue(source.contains(
                "correlationId()"
        ));
    }
}

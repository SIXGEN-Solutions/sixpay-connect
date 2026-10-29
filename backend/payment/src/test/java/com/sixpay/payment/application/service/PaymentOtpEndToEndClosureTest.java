package com.sixpay.payment.application.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PaymentOtpEndToEndClosureTest {
    private static String source(String p) throws Exception {
        return Files.readString(Path.of("src/main/java").resolve(p));
    }

    @Test
    void initiationCreatesInitialChallengeAutomatically() throws Exception {
        String x=source("com/sixpay/payment/application/service/PaymentInitiationOrchestrationService.java");
        assertTrue(x.contains("confirmation.createBefore("));
        assertTrue(x.contains("PaymentStatus.PENDING_CONFIRMATION"));
        assertTrue(x.contains("PaymentInitiationDeadline"));
    }

    @Test
    void verifyAndResendUseAuthoritativeBankOutcomes() throws Exception {
        String x=source("com/sixpay/payment/application/service/PaymentConfirmationService.java");
        String f=source("com/sixpay/payment/application/confirmation/PaymentConfirmationChallengeFactory.java");
        assertTrue(x.contains("confirmationGateway.verify("));
        assertTrue(x.contains("confirmationGateway.replace("));
        assertTrue(x.contains("PaymentConfirmationChallengeFactory.fromBankResult("));
        assertTrue(f.contains("result.status()"));
    }

    @Test
    void ambiguousOutcomeUsesRecovery() throws Exception {
        String x=source("com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationIdempotencyAdapter.java");
        assertTrue(x.contains("PaymentConfirmationGateway.OutcomeUnknownException"));
        assertTrue(x.contains("transactions.markOutcomeUnknown("));
        assertTrue(x.contains("case OUTCOME_UNKNOWN ->"));
        assertTrue(x.contains("return recover("));
    }

    @Test
    void verifySupportsIdempotentReplay() throws Exception {
        String x=source("com/sixpay/payment/infrastructure/idempotency/PaymentConfirmationIdempotencyAdapter.java");
        assertTrue(x.contains("executeVerify("));
        assertTrue(x.contains("case REPLAY ->"));
        assertTrue(x.contains("PaymentConfirmationIdempotencyResult.replayed("));
    }

    @Test
    void verifiedOtpStartsAuthorizationRecheck() throws Exception {
        String x=source("com/sixpay/payment/application/service/PaymentConfirmationService.java");
        assertTrue(x.contains("ConfirmationChallengeStatus.VERIFIED"));
        assertTrue(x.contains("authorizationService.startAuthorization("));
    }

    @Test
    void publicTransportHasNoManualCreate() throws Exception {
        String x=source("com/sixpay/payment/api/PaymentConfirmationController.java");
        assertFalse(x.contains("createPaymentConfirmationChallenge"));
        assertFalse(x.contains("SCOPE_payment.confirmation.create"));
        assertTrue(x.contains("getPaymentConfirmationChallenge"));
        assertTrue(x.contains("verifyPaymentConfirmationChallenge"));
        assertTrue(x.contains("resendPaymentConfirmationChallenge"));
    }
}

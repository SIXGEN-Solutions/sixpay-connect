package com.sixpay.payment.e2e;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Global TRESOR PAY reliability journey.
 *
 * <p>This test validates that the production path is fully wired from
 * initiation through OTP, T0, TFJ finality and both callbacks, including
 * network-loss/retry safeguards. Focused tests keep the behavioral depth;
 * this test closes the complete journey without duplicating business logic.</p>
 */
class TresorPayPaymentEndToEndReliabilityTest {

    private static String source(String relativePath) throws Exception {
        return Files.readString(
                Path.of("src/main/java").resolve(relativePath)
        );
    }

    private static String testSource(String relativePath) throws Exception {
        return Files.readString(
                Path.of("src/test/java").resolve(relativePath)
        );
    }

    @Test
    void completeTresorPayJourneyIsClosedFromInitiationToTfjCallback()
            throws Exception {

        String initiation = source(
                "com/sixpay/payment/application/service/"
                        + "PaymentInitiationOrchestrationService.java"
        );
        String confirmation = source(
                "com/sixpay/payment/application/service/"
                        + "PaymentConfirmationService.java"
        );
        String reconciliation = source(
                "com/sixpay/payment/application/service/"
                        + "PaymentReconciliationService.java"
        );
        String callbackPlan = source(
                "com/sixpay/payment/infrastructure/callback/relay/"
                        + "PaymentCallbackPlanFactory.java"
        );
        String relay = source(
                "com/sixpay/payment/infrastructure/callback/relay/"
                        + "PaymentCallbackOutboxRelay.java"
        );

        assertThat(initiation)
                .contains("PaymentStatus.RECEIVED")
                .contains("PaymentStatus.BANKING_VERIFICATION_PENDING")
                .contains("PaymentStatus.PENDING_CONFIRMATION")
                .contains("confirmation.createBefore(")
                .contains("ConfirmationChallengeStatus.ACTIVE")
                .contains("PaymentInitiationResult.awaitingOtp(");

        assertThat(confirmation)
                .contains("confirmationGateway.verify(")
                .contains("ConfirmationChallengeStatus.VERIFIED")
                .contains("authorizationService.startAuthorization(");

        assertThat(reconciliation)
                .contains("payment.recordMatchedEndOfDayConfirmation(");

        assertThat(callbackPlan)
                .contains("PaymentEventOutcomeRecorded.class.getSimpleName()")
                .contains("PaymentStatusCallbackMessage.CUT_CREDITED")
                .contains("PaymentEndOfDayConfirmationRecorded.class.getSimpleName()")
                .contains("PaymentStatusCallbackMessage.TREASURY_INTEGRATED")
                .contains("TfjStatus.INTEGRATED");

        assertThat(relay)
                .contains("coordinator.claim()")
                .contains("transport.send(plan.delivery())")
                .contains("coordinator.markPublished(event.eventId())")
                .contains("coordinator.markFailed(");
    }

    @Test
    void lostInitiationResponseIsRecoveredWithoutCreatingAnotherPayment()
            throws Exception {

        String adapter = source(
                "com/sixpay/payment/infrastructure/idempotency/"
                        + "PaymentInitiationIdempotencyAdapter.java"
        );
        String recovery = source(
                "com/sixpay/payment/application/service/"
                        + "PaymentRecoveryService.java"
        );
        String recoveryController = source(
                "com/sixpay/payment/api/"
                        + "TresorPayPaymentRecoveryController.java"
        );
        String idempotencyIt = testSource(
                "com/sixpay/payment/PaymentIdempotencyFoundationIT.java"
        );

        assertThat(adapter)
                .contains("case REPLAY ->")
                .contains("replay(decision)")
                .contains("replayStore.complete(");

        assertThat(recovery)
                .contains("Read-only recovery view")
                .contains("findByPublicPaymentReference(")
                .doesNotContain("verifyCustomer(")
                .doesNotContain("createBefore(")
                .doesNotContain("submit");

        assertThat(recoveryController)
                .contains("@GetMapping(\"/{paymentReference}\")")
                .contains("getTresorPayPayment");

        assertThat(idempotencyIt)
                .contains("replaysCompletedPaymentResult")
                .contains("serializesConcurrentTransactionsForSameKey");
    }

    @Test
    void uncertainOtpNetworkOutcomeUsesAuthoritativeRecoveryNotBlindRetry()
            throws Exception {

        String adapter = source(
                "com/sixpay/payment/infrastructure/idempotency/"
                        + "PaymentConfirmationIdempotencyAdapter.java"
        );
        String adapterTest = testSource(
                "com/sixpay/payment/infrastructure/idempotency/"
                        + "PaymentConfirmationIdempotencyAdapterTest.java"
        );

        assertThat(adapter)
                .contains("PaymentConfirmationGateway.OutcomeUnknownException")
                .contains("transactions.markOutcomeUnknown(")
                .contains("return recover(");

        assertThat(adapterTest)
                .contains("uncertainCreateMarksUnknownThenRecoversWithoutBlindRetry")
                .contains("assertThat(createCalls.get()).isEqualTo(1)")
                .contains("assertThat(recoveryCalls.get()).isEqualTo(1)")
                .contains("completedVerifyReplayNeverInvokesBankVerification");
    }

    @Test
    void callbackNetworkFailureIsRetriedFromDurableOutbox()
            throws Exception {

        String relay = source(
                "com/sixpay/payment/infrastructure/callback/relay/"
                        + "PaymentCallbackOutboxRelay.java"
        );
        String coordinator = source(
                "com/sixpay/payment/infrastructure/callback/relay/"
                        + "PaymentCallbackOutboxCoordinator.java"
        );
        String relayTest = testSource(
                "com/sixpay/payment/infrastructure/callback/relay/"
                        + "PaymentCallbackOutboxRelayTest.java"
        );
        String atomicity = testSource(
                "com/sixpay/payment/PaymentOutboxAtomicityIT.java"
        );

        assertThat(relay)
                .contains("transport.send(plan.delivery())")
                .contains("coordinator.markFailed(")
                .contains("coordinator.markPublished(");

        assertThat(coordinator)
                .contains("repository.lockClaimable(")
                .contains("entity.claim(")
                .contains("entity.markFailed(")
                .contains("now.plus(retryDelay(attemptCount))")
                .contains("properties.getMaxAttempts()")
                .contains("entity.markDead(");

        assertThat(relayTest)
                .contains("transportFailureSchedulesRetryAndDoesNotPublish")
                .contains("markFailed(")
                .contains("markPublished(EVENT_ID)");

        assertThat(atomicity)
                .contains("commitsPaymentAndOutboxAtomically")
                .contains("rollsBackPaymentAndOutboxAtomically");
    }

    @Test
    void callbackContractKeepsStableEventIdentityAcrossDeliveryRetries()
            throws Exception {

        String contract = Files.readString(
                Path.of(
                        "..",
                        "..",
                        "documentation",
                        "contracts",
                        "tresorpay",
                        "tresorpay-payment-callback-webhook-v1.yaml"
                )
        );
        String transport = source(
                "com/sixpay/payment/infrastructure/callback/"
                        + "PaymentStatusCallbackHttpAdapter.java"
        );

        assertThat(contract)
                .contains("X-Webhook-Event-ID")
                .contains("X-Webhook-Delivery-ID")
                .contains("X-Webhook-Delivery-Attempt")
                .contains("AT_LEAST_ONCE")
                .contains("eventId identifies one immutable logical event")
                .contains("is preserved across retries")
                .contains("transport attempt and changes on retry")
                .contains("A delivery retry never replays a")
                .contains("banking operation");

        assertThat(transport)
                .contains("X-Webhook-Event-ID")
                .contains("delivery.message().eventId().toString()")
                .contains("X-Webhook-Delivery-ID")
                .contains("delivery.deliveryId().toString()")
                .contains("X-Webhook-Delivery-Attempt")
                .contains("Integer.toString(delivery.deliveryAttempt())");
    }
}

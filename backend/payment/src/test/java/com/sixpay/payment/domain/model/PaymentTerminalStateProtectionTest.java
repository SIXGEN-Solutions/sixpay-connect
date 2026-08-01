package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.exception.PaymentDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTerminalStateProtectionTest {

    @Test
    void allFourTerminalStatesRejectFurtherMutation() {
        assertTerminal(rejectedPayment());
        assertTerminal(failedPayment());
        assertTerminal(treasuryIntegratedPayment());
        assertTerminal(reversedPayment());
    }

    private static void assertTerminal(Payment payment) {
        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        assertThrows(
                PaymentDomainException.class,
                () -> payment.startAuthorizationChecking(
                        before.updatedAt().plusSeconds(1)
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    private static Payment rejectedPayment() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        payment.reject(
                PaymentAggregateTestFixtures.businessFailure(
                        "TERMINAL_REJECTED",
                        FailureStage.INTAKE,
                        PaymentAggregateTestFixtures.T0.plusSeconds(1)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(1),
                PaymentAggregateTestFixtures.profiles()
        );
        return payment;
    }

    private static Payment failedPayment() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        payment.failWithoutFinancialEffect(
                PaymentAggregateTestFixtures.technicalFailure(
                        "TERMINAL_FAILED",
                        FailureStage.INTAKE,
                        PaymentAggregateTestFixtures.T0.plusSeconds(1)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(1),
                PaymentAggregateTestFixtures.profiles()
        );
        return payment;
    }

    private static Payment treasuryIntegratedPayment() {
        Payment payment =
                PaymentAggregateTestFixtures.postedPendingTfjPayment();
        payment.recordMatchedEndOfDayConfirmation(
                PaymentAggregateTestFixtures.tfjIntegrated("8"),
                PaymentAggregateTestFixtures.tfjProof(),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(12),
                PaymentAggregateTestFixtures.profiles()
        );
        return payment;
    }

    private static Payment reversedPayment() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();
        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .reversalRequiredPosting("7"),
                PaymentAggregateTestFixtures
                        .reversalRequiredFailure(),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );
        payment.authorizeReversal(
                PaymentAggregateTestFixtures.reversalInstruction(),
                PaymentAggregateTestFixtures.reversalAuthorization(),
                PaymentAggregateTestFixtures.T0.plusSeconds(13),
                PaymentAggregateTestFixtures.profiles()
        );
        payment.recordReversalOutcome(
                PaymentAggregateTestFixtures.reversedSnapshot("8"),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(16),
                PaymentAggregateTestFixtures.profiles()
        );
        return payment;
    }
}

package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.event.PaymentConfirmationChallengeRecorded;
import com.sixpay.payment.domain.event.PaymentDebtorAccountResolved;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentSubordinateMutationDomainEventTest {

    @Test
    void debtorAccountResolutionEmitsOneSafeDomainEventAndIncrementsVersion() {
        Payment initial = PaymentAggregateTestFixtures.newPayment();
        Payment withoutAccount = Payment.reconstitute(
                initial.toState().toBuilder().debtorAccountReference(null).build());
        withoutAccount.startBankingVerification(
                PaymentAggregateTestFixtures.T0.plusSeconds(1));
        Payment payment = Payment.reconstitute(withoutAccount.toState());

        DebtorAccountReference account = new DebtorAccountReference(
                PaymentAggregateTestFixtures.BANK,
                "vault:debtor:0001",
                "****************1234",
                PaymentAggregateTestFixtures.ACCOUNT_FINGERPRINT);
        long previousVersion = payment.businessVersion();
        payment.resolveDebtorAccount(
                account, PaymentAggregateTestFixtures.T0.plusSeconds(2));

        assertEquals(previousVersion + 1, payment.businessVersion());
        assertEquals(1, payment.domainEvents().size());
        PaymentDebtorAccountResolved event = assertInstanceOf(
                PaymentDebtorAccountResolved.class, payment.domainEvents().get(0));
        assertEquals(payment.businessVersion(), event.aggregateVersion());
        assertEquals(PaymentStatus.BANKING_VERIFICATION_PENDING, event.paymentStatus());
        assertEquals(account.bindingFingerprint(), event.accountBindingFingerprint());
        assertEquals(account.maskedDisplay(), event.maskedDebtorAccountReference());
        assertFalse(event.toString().contains(account.integrationAccountToken()));

        payment.resolveDebtorAccount(
                account, PaymentAggregateTestFixtures.T0.plusSeconds(3));
        assertEquals(previousVersion + 1, payment.businessVersion());
        assertEquals(1, payment.domainEvents().size());
    }

    @Test
    void challengeRecordingAndReplacementEmitEventsButExactReplayDoesNot() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        payment.startBankingVerification(PaymentAggregateTestFixtures.T0.plusSeconds(1));
        payment.recordBankingVerification(
                PaymentAggregateTestFixtures.bankingVerified("4"),
                null, PaymentAggregateTestFixtures.T0.plusSeconds(2),
                PaymentAggregateTestFixtures.profiles());
        payment = Payment.reconstitute(payment.toState());

        ConfirmationChallenge verified =
                PaymentAggregateTestFixtures.verifiedConfirmationChallenge(payment);
        ConfirmationChallenge active = new ConfirmationChallenge(
                verified.challengeReference(), verified.binding(),
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                null, null, null, null);

        long initialVersion = payment.businessVersion();
        payment.recordConfirmationChallenge(
                active, PaymentAggregateTestFixtures.T0.plusSeconds(3));
        assertEquals(initialVersion + 1, payment.businessVersion());
        assertEquals(1, payment.domainEvents().size());
        PaymentConfirmationChallengeRecorded recorded = assertInstanceOf(
                PaymentConfirmationChallengeRecorded.class, payment.domainEvents().get(0));
        assertEquals(ConfirmationChallengeStatus.ACTIVE, recorded.challengeStatus());
        assertEquals(ConfirmationBusinessCode.CHALLENGE_ACTIVE, recorded.businessCode());
        assertEquals(payment.businessVersion(), recorded.aggregateVersion());
        assertFalse(recorded.toString().contains(active.challengeReference().toString()));
        assertFalse(recorded.toString().contains(active.binding().debtorAccountReference()));

        payment.recordConfirmationChallenge(
                active, PaymentAggregateTestFixtures.T0.plusSeconds(4));
        assertEquals(initialVersion + 1, payment.businessVersion());
        assertEquals(1, payment.domainEvents().size());

        ConfirmationChallenge replacement = new ConfirmationChallenge(
                new ConfirmationChallengeReference("CHALLENGE-REPLACEMENT-001"),
                active.binding(), ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_REPLACED,
                null, null, null, null);
        payment.recordConfirmationChallenge(
                replacement, PaymentAggregateTestFixtures.T0.plusSeconds(5));
        assertEquals(initialVersion + 2, payment.businessVersion());
        assertEquals(2, payment.domainEvents().size());
        assertInstanceOf(PaymentConfirmationChallengeRecorded.class,
                payment.domainEvents().get(1));
        assertEquals(replacement, payment.toState().confirmationChallenge().orElseThrow());
    }
}

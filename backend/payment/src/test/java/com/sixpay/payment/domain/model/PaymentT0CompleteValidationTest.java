package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentT0CompleteValidationTest {

    @Test
    void otpVerifiedAuthorizationApprovedAndCoreBankingCompletedEndsPostedPendingTfj() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(7)
        );

        payment.recordPaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.COMPLETED,
                        PaymentEventObservationSource.DIRECT_RESPONSE,
                        null
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
    }

    @Test
    void insufficientFundsRejectsPayment() {
        assertRejected(
                FundsControlCheckType.AVAILABLE_FUNDS_SUFFICIENT,
                "INSUFFICIENT_FUNDS"
        );
    }

    @Test
    void inactiveAccountRejectsPayment() {
        assertRejected(
                FundsControlCheckType.ACCOUNT_ACTIVE,
                "ACCOUNT_INACTIVE"
        );
    }

    @Test
    void debitProhibitedRejectsPayment() {
        assertRejected(
                FundsControlCheckType.DEBIT_ALLOWED,
                "DEBIT_NOT_ALLOWED"
        );
    }

    @Test
    void unsupportedCurrencyRejectsPayment() {
        assertRejected(
                FundsControlCheckType.CURRENCY_SUPPORTED,
                "CURRENCY_NOT_SUPPORTED"
        );
    }

    @Test
    void perTransactionLimitRejectsPayment() {
        assertRejected(
                FundsControlCheckType.PER_TRANSACTION_LIMIT_NOT_EXCEEDED,
                "PER_TRANSACTION_LIMIT_EXCEEDED"
        );
    }

    @Test
    void dailyLimitRejectsPayment() {
        assertRejected(
                FundsControlCheckType.DAILY_LIMIT_NOT_EXCEEDED,
                "DAILY_LIMIT_EXCEEDED"
        );
    }

    @Test
    void otherLimitRejectsPayment() {
        assertRejected(
                FundsControlCheckType.OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED,
                "OTHER_LIMIT_EXCEEDED"
        );
    }

    @Test
    void invalidCreditorAccountRejectsPaymentWithoutInventingNinthExecutionCheck() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(7)
        );

        String reasonCode = "CREDITOR_ACCOUNT_NOT_USABLE";

        payment.recordPaymentEventOutcome(
                rejectedOutcome(
                        instruction,
                        FundsControlCheckType.ACCOUNT_EXISTS,
                        reasonCode
                ),
                rejectionFailure(
                        reasonCode,
                        PaymentAggregateTestFixtures.T0.plusSeconds(8)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REJECTED, payment.status());
        assertEquals(
                reasonCode,
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .reasonCode()
                        .orElseThrow()
                        .value()
        );
    }

    @Test
    void timeoutAfterTransmissionMovesToUnknownThenPaymentReferenceLookupCompletes() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(7)
        );

        payment.recordPaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.UNKNOWN,
                        PaymentEventObservationSource.DIRECT_RESPONSE,
                        null
                ),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(8)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                payment.status()
        );

        payment.resolvePaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.COMPLETED,
                        PaymentEventObservationSource.PAYMENT_REFERENCE_LOOKUP,
                        null
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(9),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
        assertEquals(
                PaymentEventObservationSource.PAYMENT_REFERENCE_LOOKUP,
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .observationSource()
        );
    }

    @Test
    void timeoutThenIdempotencyLookupCompletesWithSamePostingIdentity() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(7)
        );

        payment.recordPaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.UNKNOWN,
                        PaymentEventObservationSource.DIRECT_RESPONSE,
                        null
                ),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(8)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        payment.resolvePaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.COMPLETED,
                        PaymentEventObservationSource.IDEMPOTENCY_LOOKUP,
                        null
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(10),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
        assertEquals(
                instruction.idempotencyKey(),
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .postingCommandIdempotencyKey()
        );
        assertEquals(
                PaymentEventObservationSource.IDEMPOTENCY_LOOKUP,
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .observationSource()
        );
    }

    private static void assertRejected(
            FundsControlCheckType failedCheck,
            String reasonCode
    ) {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(7)
        );

        payment.recordPaymentEventOutcome(
                rejectedOutcome(
                        instruction,
                        failedCheck,
                        reasonCode
                ),
                rejectionFailure(
                        reasonCode,
                        PaymentAggregateTestFixtures.T0.plusSeconds(8)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REJECTED, payment.status());
        assertEquals(
                reasonCode,
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .reasonCode()
                        .orElseThrow()
                        .value()
        );
    }

    private static PaymentFailure rejectionFailure(
            String reasonCode,
            java.time.Instant observedAt
    ) {
        return new PaymentFailure(
                FailureCode.of(reasonCode),
                FailureCategory.BUSINESS_REJECTION,
                FailureStage.POSTING,
                RetryDisposition.NOT_RETRYABLE,
                "Core Banking rejected Payment event",
                observedAt,
                ExternalSystem.AMPLITUDE
        );
    }

    private static PaymentEventOutcomeSnapshot rejectedOutcome(
            PostingInstructionIdentity instruction,
            FundsControlCheckType failedCheck,
            String reasonCode
    ) {
        List<FundsControlCheckEvidence> checks =
                Arrays.stream(FundsControlCheckType.values())
                        .map(type ->
                                new FundsControlCheckEvidence(
                                        type,
                                        type == failedCheck
                                                ? EvidenceCheckResult.FAIL
                                                : EvidenceCheckResult.PASS,
                                        type == failedCheck
                                                ? FailureCode.of(reasonCode)
                                                : null,
                                        PaymentAggregateTestFixtures.T0
                                                .plusSeconds(8)
                                )
                        )
                        .toList();

        return new PaymentEventOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PaymentEventOutcome.REJECTED,
                checks,
                null,
                FailureCode.of(reasonCode),
                LocalDate.of(2026, 9, 6),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentEventObservationSource.DIRECT_RESPONSE
        );
    }

    private static PaymentEventOutcomeSnapshot outcome(
            PostingInstructionIdentity instruction,
            PaymentEventOutcome outcome,
            PaymentEventObservationSource source,
            String reasonCode
    ) {
        List<FundsControlCheckEvidence> checks =
                Arrays.stream(FundsControlCheckType.values())
                        .map(type ->
                                new FundsControlCheckEvidence(
                                        type,
                                        outcome == PaymentEventOutcome.UNKNOWN
                                                ? EvidenceCheckResult.UNKNOWN
                                                : EvidenceCheckResult.PASS,
                                        null,
                                        PaymentAggregateTestFixtures.T0
                                                .plusSeconds(8)
                                )
                        )
                        .toList();

        return new PaymentEventOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                outcome,
                checks,
                outcome == PaymentEventOutcome.COMPLETED
                        ? BankPostingReference.principalOnly("BANK-T0-001")
                        : null,
                reasonCode == null ? null : FailureCode.of(reasonCode),
                LocalDate.of(2026, 9, 6),
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                source
        );
    }
}

package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.*;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PaymentAggregateTestFixtures {

    static final Instant T0 =
            Instant.parse("2026-07-31T12:00:00Z");
    static final FinancialInstitutionCode BANK =
            FinancialInstitutionCode.of("BANK_CM");
    static final String ACCOUNT_FINGERPRINT =
            "v1:" + "a".repeat(64);
    static final Money AMOUNT =
            Money.of(new BigDecimal("1000"), "XAF");

    private PaymentAggregateTestFixtures() {
    }

    public static Payment newPayment() {
        return Payment.receive(
                new PaymentId(
                        UUID.fromString(
                                "5ee1764d-3b5f-4dd6-a13b-718f0555be83"
                        )
                ),
                PublicPaymentReference.of(
                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                ),
                newIntent(),
                T0
        );
    }


    static Payment authorizationCheckingPayment() {
        Payment payment = newPayment();
        payment.startBankingVerification(T0.plusSeconds(1));
        payment.recordBankingVerification(
                bankingVerified("4"),
                null,
                T0.plusSeconds(2),
                profiles()
        );
        payment.recordCustomerConfirmation(
                verifiedConfirmationChallenge(payment)
        );
        return payment;
    }

    static ConfirmationChallenge verifiedConfirmationChallenge(
            Payment payment
    ) {
        return new ConfirmationChallenge(
                new ConfirmationChallengeReference(
                        "CHALLENGE-VERIFIED-001"
                ),
                new ConfirmationChallengeBinding(
                        payment.publicPaymentReference(),
                        "CUSTOMER-001",
                        "vault:debtor:0001",
                        AMOUNT
                ),
                ConfirmationChallengeStatus.VERIFIED,
                ConfirmationBusinessCode.OTP_VERIFIED,
                null,
                null,
                null,
                T0.plusSeconds(3)
        );
    }

    static Payment approvedPayment() {
        Payment payment = authorizationCheckingPayment();
        PaymentPolicyBundle profiles = profiles();
        payment.recordAuthorizationDecision(
                authorizationApproved("3"),
                null,
                T0.plusSeconds(4),
                profiles
        );
        payment.recordFundsControl(
                fundsVerified("5"),
                null,
                T0.plusSeconds(5),
                profiles
        );
        payment.recordTreasuryAccountResolution(
                treasuryResolved("6"),
                treasuryAccount(),
                null,
                T0.plusSeconds(6),
                profiles
        );
        return payment;
    }

    static Payment postingPendingPayment() {
        Payment payment = approvedPayment();
        payment.authorizePosting(
                postingInstruction(),
                T0.plusSeconds(6),
                profiles()
        );
        return payment;
    }

    static Payment postedPendingTfjPayment() {
        Payment payment = postingPendingPayment();
        payment.recordPostingOutcome(
                completedPosting(
                        "7",
                        EvidenceObservationChannel.DIRECT_RESPONSE
                ),
                null,
                T0.plusSeconds(7),
                profiles()
        );
        return payment;
    }

    static NewPaymentIntent newIntent() {
        return new NewPaymentIntent(
                PaymentSource.TRESOR_PAY,
                ExternalPaymentReference.of("PAYMENT-001"),
                ExternalSubscriptionReference.of("SUBSCRIPTION-001"),
                new PaymentRequestIdentity(
                        IdempotencyKey.of("PAYMENT-REQUEST-001"),
                        RequestFingerprint.of("b".repeat(64)),
                        CorrelationId.of(
                                "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                        )
                ),
                BANK,
                new DebtorAccountReference(
                        BANK,
                        "vault:debtor:0001",
                        "****************1234",
                        ACCOUNT_FINGERPRINT
                ),
                AMOUNT,
                new TreasuryAllocationIntent(
                        List.of(
                                new TreasuryAllocation(
                                        TreasuryBeneficiaryReference.of(
                                                "TAX_A"
                                        ),
                                        AMOUNT
                                )
                        ),
                        AMOUNT
                ),
                fingerprint("c"),
                new PaymentInitiationContext(
                        "TRESOR_PAY",
                        "TP_APP_001",
                        "TEST CUSTOMER",
                        ClaimType.AVI,
                        "NIU-TEST-001",
                        T0,
                        CallbackEndpoint.of(
                                "https://tresorpay.example.test/callback"
                        )
                )
        );
    }

    static AuthorizationEvidenceSnapshot authorizationApproved(
            String hex
    ) {
        return new AuthorizationEvidenceSnapshot(
                new AuthorizationEvidenceReference(
                        "v1:hmac-sha256:" + "d".repeat(64)
                ),
                AuthorizationDecisionOutcome.APPROVED,
                fingerprint("e"),
                "issuer",
                "key-01",
                "RS256",
                "payment:initiate",
                List.of(
                        new AuthorizationBindingEvidence(
                                AuthorizationBindingType.SUBSCRIPTION_REFERENCE,
                                AuthorizationBindingResult.MATCH
                        ),
                        new AuthorizationBindingEvidence(
                                AuthorizationBindingType.CLIENT_APPLICATION,
                                AuthorizationBindingResult.MATCH
                        ),
                        new AuthorizationBindingEvidence(
                                AuthorizationBindingType.PAYMENT_SCOPE,
                                AuthorizationBindingResult.MATCH
                        ),
                        new AuthorizationBindingEvidence(
                                AuthorizationBindingType.DEBTOR_ACCOUNT,
                                AuthorizationBindingResult.MATCH
                        )
                ),
                T0.minusSeconds(60),
                T0.minusSeconds(60),
                T0.plusSeconds(3600),
                null,
                metadata(
                        ExternalSystem.TRESOR_PAY,
                        EvidenceObservationChannel.LOCAL_VALIDATION,
                        hex,
                        T0.plusSeconds(1)
                )
        );
    }

    static BankingVerificationSnapshot bankingVerified(String hex) {
        return new BankingVerificationSnapshot(
                new BankingVerificationId(
                        UUID.fromString(
                                "0e30f18e-45d8-4c4d-8f18-3114d81fc60e"
                        )
                ),
                BankingVerificationOutcome.VERIFIED,
                ACCOUNT_FINGERPRINT,
                List.of(
                        bankingCheck(
                                BankingVerificationCheckType
                                        .CUSTOMER_EXISTS
                        ),
                        bankingCheck(
                                BankingVerificationCheckType
                                        .ACCOUNT_EXISTS
                        )
                ),
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(2)
                )
        );
    }

    static FundsControlSnapshot fundsVerified(String hex) {
        return new FundsControlSnapshot(
                new FundsVerificationReference("FUNDS-RESULT-001"),
                FundsControlOutcome.VERIFIED,
                AMOUNT,
                ACCOUNT_FINGERPRINT,
                List.of(
                        fundsCheck(
                                FundsControlCheckType.ACCOUNT_EXISTS
                        ),
                        fundsCheck(
                                FundsControlCheckType
                                        .AVAILABLE_FUNDS_SUFFICIENT
                        )
                ),
                T0.plusSeconds(1800),
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(3)
                )
        );
    }

    static TreasuryAccountReference treasuryAccount() {
        return new TreasuryAccountReference(
                BANK,
                "CUT-CONFIG-001",
                "vault:treasury:0001",
                "****************9999",
                "v7"
        );
    }

    static TreasuryAccountResolutionSnapshot treasuryResolved(
            String hex
    ) {
        return new TreasuryAccountResolutionSnapshot(
                treasuryAccount(),
                fingerprint("c"),
                TreasuryResolutionOutcome.RESOLVED,
                "policy-v7",
                null,
                metadata(
                        ExternalSystem.SIXPAY,
                        EvidenceObservationChannel
                                .PROTECTED_CONFIGURATION_RESOLUTION,
                        hex,
                        T0.plusSeconds(4)
                )
        );
    }

    static PostingInstructionIdentity postingInstruction() {
        return new PostingInstructionIdentity(
                new PostingInstructionId(
                        UUID.fromString(
                                "80579bb5-af3c-46bd-a2fc-d30fb2672aed"
                        )
                ),
                new PostingIdempotencyKey(
                        "POSTING-IDEMPOTENCY-001"
                ),
                AMOUNT,
                ACCOUNT_FINGERPRINT,
                fingerprint("1")
        );
    }

    static PostingOutcomeSnapshot completedPosting(
            String hex,
            EvidenceObservationChannel channel
    ) {
        PostingInstructionIdentity instruction =
                postingInstruction();
        return new PostingOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PostingOutcome.COMPLETED,
                bankPostingReference(),
                successfulLeg("DEBIT-001"),
                successfulLeg("CUT-001"),
                AMOUNT,
                LocalDate.of(2026, 7, 31),
                null,
                PostingNextAction.NONE,
                metadata(
                        ExternalSystem.AMPLITUDE,
                        channel,
                        hex,
                        T0.plusSeconds(6)
                )
        );
    }

    static PostingOutcomeSnapshot unknownPosting(String hex) {
        PostingInstructionIdentity instruction =
                postingInstruction();
        return new PostingOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PostingOutcome.UNKNOWN,
                null,
                new PostingLegEvidence(
                        PostingLegStatus.UNKNOWN,
                        null,
                        null,
                        null
                ),
                new PostingLegEvidence(
                        PostingLegStatus.UNKNOWN,
                        null,
                        null,
                        null
                ),
                AMOUNT,
                LocalDate.of(2026, 7, 31),
                null,
                PostingNextAction.QUERY_OUTCOME,
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(6)
                )
        );
    }



    static PostingOutcomeSnapshot debitConfirmedPosting(String hex) {
        PostingInstructionIdentity instruction = postingInstruction();
        return new PostingOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PostingOutcome
                        .DEBIT_CONFIRMED_CUT_CREDIT_PENDING,
                bankPostingReference(),
                successfulLeg("DEBIT-001"),
                new PostingLegEvidence(
                        PostingLegStatus.PENDING,
                        null,
                        null,
                        null
                ),
                AMOUNT,
                LocalDate.of(2026, 7, 31),
                null,
                PostingNextAction.WAIT_FOR_CUT_CREDIT,
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(7)
                )
        );
    }

    static PostingOutcomeSnapshot rejectedPostingWithoutEffect(
            String hex,
            String failureCode
    ) {
        PostingInstructionIdentity instruction = postingInstruction();
        FailureCode code = FailureCode.of(failureCode);
        return new PostingOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PostingOutcome.REJECTED_NO_FINANCIAL_EFFECT,
                null,
                new PostingLegEvidence(
                        PostingLegStatus.FAILED,
                        null,
                        null,
                        code
                ),
                new PostingLegEvidence(
                        PostingLegStatus.NOT_STARTED,
                        null,
                        null,
                        null
                ),
                AMOUNT,
                LocalDate.of(2026, 7, 31),
                code,
                PostingNextAction.NONE,
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(7)
                )
        );
    }

    static ReversalSnapshot unknownReversalSnapshot(String hex) {
        ReversalInstructionIdentity instruction =
                reversalInstruction();
        return new ReversalSnapshot(
                bankPostingReference(),
                instruction.instructionId(),
                instruction.idempotencyKey(),
                reversalAuthorization(),
                new ReversalOutcomeEvidence(
                        null,
                        ReversalOutcome.UNKNOWN,
                        null,
                        null,
                        metadata(
                                ExternalSystem.AMPLITUDE,
                                EvidenceObservationChannel.DIRECT_RESPONSE,
                                hex,
                                T0.plusSeconds(15)
                        )
                )
        );
    }

    static ReversalSnapshot resolvedReversalSnapshot(String hex) {
        ReversalInstructionIdentity instruction =
                reversalInstruction();
        return new ReversalSnapshot(
                bankPostingReference(),
                instruction.instructionId(),
                instruction.idempotencyKey(),
                reversalAuthorization(),
                new ReversalOutcomeEvidence(
                        new ReversalReference("REVERSAL-001"),
                        ReversalOutcome.REVERSED,
                        "REVERSAL-ENTRY-001",
                        null,
                        metadata(
                                ExternalSystem.AMPLITUDE,
                                EvidenceObservationChannel
                                        .BANK_REFERENCE_LOOKUP,
                                hex,
                                T0.plusSeconds(16)
                        )
                )
        );
    }

    static PostingOutcomeSnapshot reversalRequiredPosting(String hex) {
        PostingInstructionIdentity instruction = postingInstruction();
        FailureCode code = FailureCode.of("CUT_CREDIT_FAILED");
        return new PostingOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                PostingOutcome.REVERSAL_REQUIRED,
                bankPostingReference(),
                successfulLeg("DEBIT-001"),
                new PostingLegEvidence(
                        PostingLegStatus.FAILED,
                        null,
                        null,
                        code
                ),
                AMOUNT,
                LocalDate.of(2026, 7, 31),
                code,
                PostingNextAction.REQUEST_EXPLICIT_REVERSAL,
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        hex,
                        T0.plusSeconds(7)
                )
        );
    }

    static PaymentFailure reversalRequiredFailure() {
        return new PaymentFailure(
                FailureCode.of("CUT_CREDIT_FAILED"),
                FailureCategory.TECHNICAL_FAILURE,
                FailureStage.POSTING,
                RetryDisposition.RECOVERY_EVENT_REQUIRED,
                "CUT credit requires reversal",
                T0.plusSeconds(7),
                ExternalSystem.AMPLITUDE
        );
    }

    static ReversalSnapshot reversedSnapshot(String hex) {
        ReversalInstructionIdentity instruction =
                reversalInstruction();
        ReversalAuthorizationEvidence authorization =
                reversalAuthorization();

        return new ReversalSnapshot(
                bankPostingReference(),
                instruction.instructionId(),
                instruction.idempotencyKey(),
                authorization,
                new ReversalOutcomeEvidence(
                        new ReversalReference("REVERSAL-001"),
                        ReversalOutcome.REVERSED,
                        "REVERSAL-ENTRY-001",
                        null,
                        metadata(
                                ExternalSystem.AMPLITUDE,
                                EvidenceObservationChannel.DIRECT_RESPONSE,
                                hex,
                                T0.plusSeconds(15)
                        )
                )
        );
    }

    static EndOfDayConfirmationSnapshot tfjIntegrated(String hex) {
        return new EndOfDayConfirmationSnapshot(
                new TfjConfirmationId(
                        UUID.fromString(
                                "b257a2f4-4196-4e44-8fb3-061cc41fb6c4"
                        )
                ),
                BANK,
                LocalDate.of(2026, 7, 31),
                PublicPaymentReference.of(
                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                ),
                "POSTING-001",
                "TFJ-BATCH-001",
                TfjStatus.INTEGRATED,
                null,
                T0.plusSeconds(10),
                T0.plusSeconds(11),
                metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.ASYNC_CALLBACK,
                        hex,
                        T0.plusSeconds(11)
                )
        );
    }

    static UniqueTfjMatchProof tfjProof() {
        return new UniqueTfjMatchProof(
                new TfjConfirmationId(
                        UUID.fromString(
                                "b257a2f4-4196-4e44-8fb3-061cc41fb6c4"
                        )
                ),
                true,
                true,
                true
        );
    }

    static ReversalInstructionIdentity reversalInstruction() {
        return new ReversalInstructionIdentity(
                new ReversalInstructionId(
                        UUID.fromString(
                                "d493d67f-c552-42a3-bc3f-5ff8b26c1383"
                        )
                ),
                new ReversalIdempotencyKey(
                        "REVERSAL-IDEMPOTENCY-001"
                ),
                fingerprint("2")
        );
    }

    static ReversalAuthorizationEvidence reversalAuthorization() {
        return new ReversalAuthorizationEvidence(
                ReversalAuthorizationType.APPROVED_RUNBOOK,
                new ReversalAuthorizationReference(
                        "RUNBOOK-AUTH-001"
                ),
                "operator:payments",
                FailureCode.of("TFJ_REVERSAL_REQUIRED"),
                T0.plusSeconds(12),
                T0.plusSeconds(13)
        );
    }

    static PaymentFailure businessFailure(
            String code,
            FailureStage stage,
            Instant at
    ) {
        return new PaymentFailure(
                FailureCode.of(code),
                FailureCategory.BUSINESS_REJECTION,
                stage,
                RetryDisposition.NOT_RETRYABLE,
                "Business rejection",
                at,
                ExternalSystem.AMPLITUDE
        );
    }

    static PaymentFailure technicalFailure(
            String code,
            FailureStage stage,
            Instant at
    ) {
        return new PaymentFailure(
                FailureCode.of(code),
                FailureCategory.TECHNICAL_FAILURE,
                stage,
                RetryDisposition.SAFE_RETRY,
                "Technical failure",
                at,
                ExternalSystem.SIXPAY
        );
    }

    static PaymentFailure unknownFailure(
            String code,
            FailureStage stage,
            Instant at
    ) {
        return new PaymentFailure(
                FailureCode.of(code),
                FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                stage,
                RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED,
                "Authoritative lookup required",
                at,
                ExternalSystem.AMPLITUDE
        );
    }

    static PaymentPolicyBundle profiles() {
        PolicyProfileMetadata metadata = new PolicyProfileMetadata(
                "payment-default",
                "v1",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                "approval:architecture-board"
        );

        EnumMap<EvidenceCategory, Duration> ages =
                new EnumMap<>(EvidenceCategory.class);
        for (EvidenceCategory category : EvidenceCategory.values()) {
            ages.put(category, Duration.ofHours(24));
        }

        EvidenceTemporalProfile temporal =
                new EvidenceTemporalProfile(
                        metadata,
                        Duration.ofMinutes(2),
                        ages
                );

        return new PaymentPolicyBundle(
                temporal,
                new AuthorizationPolicyProfile(
                        metadata,
                        Set.of("issuer"),
                        Set.of("RS256"),
                        Set.of("payment:initiate"),
                        Set.of(
                                AuthorizationBindingType.PAYMENT_SCOPE,
                                AuthorizationBindingType.DEBTOR_ACCOUNT
                        )
                ),
                new BankingVerificationPolicyProfile(
                        metadata,
                        Set.of(
                                BankingVerificationCheckType
                                        .CUSTOMER_EXISTS,
                                BankingVerificationCheckType
                                        .ACCOUNT_EXISTS
                        ),
                        temporal
                ),
                new FundsControlPolicyProfile(
                        metadata,
                        Set.of(
                                FundsControlCheckType.ACCOUNT_EXISTS,
                                FundsControlCheckType
                                        .AVAILABLE_FUNDS_SUFFICIENT
                        ),
                        temporal
                ),
                new TreasuryResolutionPolicyProfile(
                        metadata,
                        Map.of(BANK, Set.of("v7"))
                ),
                new PostingAuthorizationPolicyProfile(
                        metadata,
                        Set.of(PaymentStatus.APPROVED_FOR_POSTING),
                        true,
                        true
                ),
                new FinancialOutcomePolicyProfile(
                        metadata,
                        Map.of(
                                EvidenceAuthority.DIRECT_RESPONSE, 1,
                                EvidenceAuthority.IDEMPOTENCY_LOOKUP, 2,
                                EvidenceAuthority.BANK_REFERENCE_LOOKUP, 3,
                                EvidenceAuthority.UNIQUE_TFJ_MATCH, 4
                        ),
                        Map.of(
                                EvidenceConclusiveness.INDETERMINATE, 1,
                                EvidenceConclusiveness.PARTIAL, 2,
                                EvidenceConclusiveness.CONCLUSIVE, 3,
                                EvidenceConclusiveness.FINAL, 4
                        )
                ),
                new TfjPolicyProfile(
                        metadata,
                        Set.of(TfjStatus.INTEGRATED, TfjStatus.FAILED),
                        Set.of(TfjRecoveryAction.REVERSAL_REQUIRED)
                ),
                new ReversalPolicyProfile(
                        metadata,
                        Set.of(
                                ReversalAuthorizationType.APPROVED_RUNBOOK
                        ),
                        Set.of("TFJ_REVERSAL_REQUIRED")
                ),
                new FailureClassificationProfile(
                        metadata,
                        Map.of(
                                FailureCategory.BUSINESS_REJECTION,
                                Set.of(RetryDisposition.NOT_RETRYABLE),
                                FailureCategory.SECURITY_REJECTION,
                                Set.of(RetryDisposition.NOT_RETRYABLE),
                                FailureCategory.TECHNICAL_FAILURE,
                                Set.of(
                                        RetryDisposition.SAFE_RETRY,
                                        RetryDisposition
                                                .RECOVERY_EVENT_REQUIRED,
                                        RetryDisposition
                                                .OPERATOR_ACTION_REQUIRED
                                ),
                                FailureCategory
                                        .UNCERTAIN_EXTERNAL_OUTCOME,
                                Set.of(
                                        RetryDisposition
                                                .AUTHORITATIVE_LOOKUP_REQUIRED
                                ),
                                FailureCategory.INTEGRATION_CONFLICT,
                                Set.of(
                                        RetryDisposition
                                                .OPERATOR_ACTION_REQUIRED
                                ),
                                FailureCategory
                                        .TREASURY_RECONCILIATION_FAILURE,
                                Set.of(
                                        RetryDisposition
                                                .RECOVERY_EVENT_REQUIRED,
                                        RetryDisposition
                                                .OPERATOR_ACTION_REQUIRED
                                )
                        )
                ),
                new ResultIntentPolicyProfile(
                        metadata,
                        Map.of(
                                PaymentStatus.REJECTED,
                                ResultIntentDecision.IMMEDIATE_REJECTED,
                                PaymentStatus.FAILED,
                                ResultIntentDecision.IMMEDIATE_FAILED,
                                PaymentStatus.DEBIT_CONFIRMED,
                                ResultIntentDecision.IMMEDIATE_PROCESSING,
                                PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                                ResultIntentDecision.IMMEDIATE_PROCESSING,
                                PaymentStatus.POSTED_PENDING_TFJ,
                                ResultIntentDecision
                                        .IMMEDIATE_POSTED_PENDING_TFJ,
                                PaymentStatus.REVERSAL_REQUIRED,
                                ResultIntentDecision
                                        .IMMEDIATE_REVERSAL_REQUIRED,
                                PaymentStatus.TREASURY_INTEGRATED,
                                ResultIntentDecision
                                        .FINAL_TREASURY_INTEGRATED,
                                PaymentStatus.REVERSED,
                                ResultIntentDecision.REVERSAL_REVERSED
                        )
                ),
                new EventDisclosureProfile(
                        metadata,
                        Map.of(),
                        Map.of(),
                        Set.of(),
                        Set.of(EventDataClassification.PUBLIC)
                )
        );
    }

    static EvidenceFingerprint fingerprint(String character) {
        return EvidenceFingerprint.of(
                "v1:sha256:" + character.repeat(64)
        );
    }

    static BankPostingReference bankPostingReference() {
        return new BankPostingReference(
                "POSTING-001",
                "DEBIT-001",
                "CUT-001"
        );
    }

    private static EvidenceMetadata metadata(
            ExternalSystem source,
            EvidenceObservationChannel channel,
            String hex,
            Instant observedAt
    ) {
        return new EvidenceMetadata(
                source,
                CorrelationId.of(
                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                ),
                channel,
                fingerprint(hex),
                observedAt,
                observedAt
        );
    }

    private static BankingVerificationCheckEvidence bankingCheck(
            BankingVerificationCheckType type
    ) {
        return new BankingVerificationCheckEvidence(
                type,
                EvidenceCheckResult.PASS,
                null,
                null
        );
    }

    private static FundsControlCheckEvidence fundsCheck(
            FundsControlCheckType type
    ) {
        return new FundsControlCheckEvidence(
                type,
                EvidenceCheckResult.PASS,
                null,
                T0.plusSeconds(3)
        );
    }

    private static PostingLegEvidence successfulLeg(String reference) {
        return new PostingLegEvidence(
                PostingLegStatus.SUCCEEDED,
                reference,
                T0.plusSeconds(6),
                null
        );
    }
}

package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

final class PolicyTestFixtures {

    static final Instant DECISION_AT =
            Instant.parse("2026-07-31T12:00:00Z");

    private PolicyTestFixtures() {
    }

    static PolicyProfileMetadata metadata(String id) {
        return new PolicyProfileMetadata(
                id,
                "v1",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                "approval:architecture-board"
        );
    }

    static EvidenceTemporalProfile temporalProfile() {
        EnumMap<EvidenceCategory, Duration> ages =
                new EnumMap<>(EvidenceCategory.class);
        for (EvidenceCategory category : EvidenceCategory.values()) {
            ages.put(category, Duration.ofHours(1));
        }

        return new EvidenceTemporalProfile(
                metadata("temporal"),
                Duration.ofMinutes(2),
                ages
        );
    }

    static AuthorizationPolicyProfile authorizationProfile() {
        return new AuthorizationPolicyProfile(
                metadata("authorization"),
                Set.of("issuer"),
                Set.of("RS256"),
                Set.of("payment:initiate"),
                Set.of(
                        AuthorizationBindingType.PAYMENT_SCOPE,
                        AuthorizationBindingType.DEBTOR_ACCOUNT
                )
        );
    }

    static BankingVerificationPolicyProfile bankingProfile() {
        return new BankingVerificationPolicyProfile(
                metadata("banking"),
                Set.of(
                        BankingVerificationCheckType.CUSTOMER_EXISTS,
                        BankingVerificationCheckType.ACCOUNT_EXISTS
                ),
                temporalProfile()
        );
    }

    static FundsControlPolicyProfile fundsProfile() {
        return new FundsControlPolicyProfile(
                metadata("funds"),
                Set.of(
                        FundsControlCheckType.ACCOUNT_EXISTS,
                        FundsControlCheckType.AVAILABLE_FUNDS_SUFFICIENT
                ),
                temporalProfile()
        );
    }

    static TreasuryResolutionPolicyProfile treasuryProfile() {
        return new TreasuryResolutionPolicyProfile(
                metadata("treasury"),
                Map.of(
                        FinancialInstitutionCode.of("BANK_CM"),
                        Set.of("v7")
                )
        );
    }

    static PostingAuthorizationPolicyProfile postingAuthorizationProfile() {
        return new PostingAuthorizationPolicyProfile(
                metadata("posting-authorization"),
                Set.of(PaymentStatus.APPROVED_FOR_POSTING),
                true,
                true
        );
    }

    static FinancialOutcomePolicyProfile financialProfile() {
        return new FinancialOutcomePolicyProfile(
                metadata("financial"),
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
        );
    }

    static TfjPolicyProfile tfjProfile() {
        return new TfjPolicyProfile(
                metadata("tfj"),
                Set.of(TfjStatus.INTEGRATED, TfjStatus.FAILED),
                Set.of(TfjRecoveryAction.REVERSAL_REQUIRED)
        );
    }

    static ReversalPolicyProfile reversalProfile() {
        return new ReversalPolicyProfile(
                metadata("reversal"),
                Set.of(ReversalAuthorizationType.APPROVED_RUNBOOK),
                Set.of("TFJ_REVERSAL_REQUIRED")
        );
    }

    static FailureClassificationProfile failureProfile() {
        return new FailureClassificationProfile(
                metadata("failure"),
                Map.of(
                        FailureCategory.BUSINESS_REJECTION,
                        Set.of(RetryDisposition.NOT_RETRYABLE),
                        FailureCategory.SECURITY_REJECTION,
                        Set.of(RetryDisposition.NOT_RETRYABLE),
                        FailureCategory.TECHNICAL_FAILURE,
                        Set.of(RetryDisposition.SAFE_RETRY),
                        FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                        Set.of(
                                RetryDisposition
                                        .AUTHORITATIVE_LOOKUP_REQUIRED
                        ),
                        FailureCategory.INTEGRATION_CONFLICT,
                        Set.of(RetryDisposition.OPERATOR_ACTION_REQUIRED),
                        FailureCategory.TREASURY_RECONCILIATION_FAILURE,
                        Set.of(RetryDisposition.RECOVERY_EVENT_REQUIRED)
                )
        );
    }

    static ResultIntentPolicyProfile resultProfile() {
        return new ResultIntentPolicyProfile(
                metadata("result"),
                Map.of(
                        PaymentStatus.AUTHORIZATION_CHECKING,
                        ResultIntentDecision.IMMEDIATE_PROCESSING,
                        PaymentStatus.REJECTED,
                        ResultIntentDecision.IMMEDIATE_REJECTED,
                        PaymentStatus.FAILED,
                        ResultIntentDecision.IMMEDIATE_FAILED,
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
        );
    }

    static EventDisclosureProfile disclosureProfile() {
        return new EventDisclosureProfile(
                metadata("disclosure"),
                Map.of(
                        "PaymentReceived",
                        Set.of(
                                "paymentReference",
                                "status"
                        )
                ),
                Map.of(
                        "paymentReference",
                        EventDataClassification.INTERNAL,
                        "status",
                        EventDataClassification.PUBLIC
                ),
                Set.of(
                        "accountToken",
                        "rawJwt",
                        "bindingFingerprint"
                ),
                Set.of(
                        EventDataClassification.PUBLIC,
                        EventDataClassification.INTERNAL
                )
        );
    }

    static EvidenceMetadata evidenceMetadata(
            EvidenceObservationChannel channel
    ) {
        return new EvidenceMetadata(
                ExternalSystem.AMPLITUDE,
                com.sixpay.common.context.CorrelationId.of(
                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                ),
                channel,
                EvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                ),
                DECISION_AT.minus(Duration.ofMinutes(5)),
                DECISION_AT.minus(Duration.ofMinutes(4))
        );
    }

    static Money amount() {
        return Money.of(new BigDecimal("1000"), "XAF");
    }

    static PaymentFailure businessFailure() {
        return new PaymentFailure(
                FailureCode.of("INSUFFICIENT_FUNDS"),
                FailureCategory.BUSINESS_REJECTION,
                FailureStage.FUNDS_CONTROL,
                RetryDisposition.NOT_RETRYABLE,
                "Insufficient funds",
                DECISION_AT,
                ExternalSystem.AMPLITUDE
        );
    }
}

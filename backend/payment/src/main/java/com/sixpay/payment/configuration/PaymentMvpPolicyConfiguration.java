package com.sixpay.payment.configuration;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.RetryDisposition;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative SIXPAY Payment policy baseline for the MVP.
 *
 * <p>The future Core Banking APIs must emit facts and evidence compatible
 * with these profiles. Connectivity settings do not belong here.</p>
 */
@Configuration(proxyBeanMethods = false)
public class PaymentMvpPolicyConfiguration {

    static final String PROFILE_ID = "payment-mvp";
    static final String PROFILE_VERSION = "v1";

    @Bean
    PaymentPolicyBundle paymentMvpPolicyBundle() {
        PolicyProfileMetadata metadata = new PolicyProfileMetadata(
                PROFILE_ID,
                PROFILE_VERSION,
                Instant.parse("2026-08-31T00:00:00Z"),
                null,
                "PAYMENT_COMPLETION:R5_POLICY_BASELINE"
        );

        EvidenceTemporalProfile temporal =
                evidenceTemporalProfile(metadata);

        return new PaymentPolicyBundle(
                temporal,
                authorizationProfile(metadata),
                bankingVerificationProfile(metadata, temporal),
                fundsControlProfile(metadata, temporal),
                treasuryResolutionProfile(metadata),
                postingAuthorizationProfile(metadata),
                financialOutcomeProfile(metadata),
                tfjProfile(metadata),
                reversalProfile(metadata),
                failureClassificationProfile(metadata),
                resultIntentProfile(metadata),
                eventDisclosureProfile(metadata)
        );
    }

    private static EvidenceTemporalProfile evidenceTemporalProfile(
            PolicyProfileMetadata metadata
    ) {
        EnumMap<EvidenceCategory, Duration> ages =
                new EnumMap<>(EvidenceCategory.class);

        ages.put(EvidenceCategory.AUTHORIZATION, Duration.ofMinutes(5));
        ages.put(EvidenceCategory.BANKING_VERIFICATION, Duration.ofMinutes(5));
        ages.put(EvidenceCategory.FUNDS_CONTROL, Duration.ofMinutes(2));
        ages.put(EvidenceCategory.TREASURY_RESOLUTION, Duration.ofMinutes(30));
        ages.put(EvidenceCategory.POSTING_OUTCOME, Duration.ofHours(24));
        ages.put(EvidenceCategory.TFJ_CONFIRMATION, Duration.ofHours(48));
        ages.put(EvidenceCategory.REVERSAL_OUTCOME, Duration.ofHours(24));

        return new EvidenceTemporalProfile(
                metadata,
                Duration.ofMinutes(2),
                ages
        );
    }

    private static AuthorizationPolicyProfile authorizationProfile(
            PolicyProfileMetadata metadata
    ) {
        return new AuthorizationPolicyProfile(
                metadata,
                Set.of("TRESOR_PAY", "SIXPAY"),
                Set.of("RS256", "PS256"),
                Set.of("payment:initiate"),
                Set.of(
                        AuthorizationBindingType.PAYMENT_SCOPE,
                        AuthorizationBindingType.DEBTOR_ACCOUNT
                )
        );
    }

    private static BankingVerificationPolicyProfile bankingVerificationProfile(
            PolicyProfileMetadata metadata,
            EvidenceTemporalProfile temporal
    ) {
        return new BankingVerificationPolicyProfile(
                metadata,
                Set.of(BankingVerificationCheckType.values()),
                temporal
        );
    }

    private static FundsControlPolicyProfile fundsControlProfile(
            PolicyProfileMetadata metadata,
            EvidenceTemporalProfile temporal
    ) {
        return new FundsControlPolicyProfile(
                metadata,
                Set.of(FundsControlCheckType.values()),
                temporal
        );
    }

    private static TreasuryResolutionPolicyProfile treasuryResolutionProfile(
            PolicyProfileMetadata metadata
    ) {
        return new TreasuryResolutionPolicyProfile(
                metadata,
                Map.of(
                        FinancialInstitutionCode.of("BANK_CM"),
                        Set.of("v1")
                )
        );
    }

    private static PostingAuthorizationPolicyProfile postingAuthorizationProfile(
            PolicyProfileMetadata metadata
    ) {
        return new PostingAuthorizationPolicyProfile(
                metadata,
                Set.of(PaymentStatus.APPROVED_FOR_POSTING),
                true,
                true
        );
    }

    private static FinancialOutcomePolicyProfile financialOutcomeProfile(
            PolicyProfileMetadata metadata
    ) {
        return new FinancialOutcomePolicyProfile(
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
        );
    }

    private static TfjPolicyProfile tfjProfile(
            PolicyProfileMetadata metadata
    ) {
        return new TfjPolicyProfile(
                metadata,
                Set.of(TfjStatus.INTEGRATED, TfjStatus.FAILED),
                Set.of(TfjRecoveryAction.REVERSAL_REQUIRED)
        );
    }

    private static ReversalPolicyProfile reversalProfile(
            PolicyProfileMetadata metadata
    ) {
        return new ReversalPolicyProfile(
                metadata,
                Set.of(ReversalAuthorizationType.APPROVED_RUNBOOK),
                Set.of("TFJ_REVERSAL_REQUIRED", "CUT_CREDIT_FAILED")
        );
    }

    private static FailureClassificationProfile failureClassificationProfile(
            PolicyProfileMetadata metadata
    ) {
        return new FailureClassificationProfile(
                metadata,
                Map.of(
                        FailureCategory.BUSINESS_REJECTION,
                        Set.of(RetryDisposition.NOT_RETRYABLE),
                        FailureCategory.SECURITY_REJECTION,
                        Set.of(RetryDisposition.NOT_RETRYABLE),
                        FailureCategory.TECHNICAL_FAILURE,
                        Set.of(
                                RetryDisposition.SAFE_RETRY,
                                RetryDisposition.RECOVERY_EVENT_REQUIRED,
                                RetryDisposition.OPERATOR_ACTION_REQUIRED
                        ),
                        FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                        Set.of(RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED),
                        FailureCategory.INTEGRATION_CONFLICT,
                        Set.of(RetryDisposition.OPERATOR_ACTION_REQUIRED),
                        FailureCategory.TREASURY_RECONCILIATION_FAILURE,
                        Set.of(
                                RetryDisposition.RECOVERY_EVENT_REQUIRED,
                                RetryDisposition.OPERATOR_ACTION_REQUIRED
                        )
                )
        );
    }

    private static ResultIntentPolicyProfile resultIntentProfile(
            PolicyProfileMetadata metadata
    ) {
        return new ResultIntentPolicyProfile(
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
                        ResultIntentDecision.IMMEDIATE_POSTED_PENDING_TFJ,
                        PaymentStatus.REVERSAL_REQUIRED,
                        ResultIntentDecision.IMMEDIATE_REVERSAL_REQUIRED,
                        PaymentStatus.TREASURY_INTEGRATED,
                        ResultIntentDecision.FINAL_TREASURY_INTEGRATED,
                        PaymentStatus.REVERSED,
                        ResultIntentDecision.REVERSAL_REVERSED
                )
        );
    }

    private static EventDisclosureProfile eventDisclosureProfile(
            PolicyProfileMetadata metadata
    ) {
        return new EventDisclosureProfile(
                metadata,
                Map.of(),
                Map.of(),
                Set.of(),
                Set.of(EventDataClassification.PUBLIC)
        );
    }
}

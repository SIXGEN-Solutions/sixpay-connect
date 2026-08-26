package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreAcceptancePoliciesTest {

    @Test
    void temporalPolicyClassifiesValidStaleAndFutureEvidence() {
        EvidenceTemporalValidityPolicy policy =
                new EvidenceTemporalValidityPolicy();

        assertEquals(
                EvidenceTemporalDecision.VALID,
                policy.decide(
                        PolicyTestFixtures.evidenceMetadata(
                                EvidenceObservationChannel.DIRECT_RESPONSE
                        ),
                        EvidenceCategory.BANKING_VERIFICATION,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.temporalProfile()
                ).decision()
        );

        EvidenceMetadata stale = new EvidenceMetadata(
                ExternalSystem.AMPLITUDE,
                com.sixpay.common.context.CorrelationId.of(
                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                ),
                EvidenceObservationChannel.DIRECT_RESPONSE,
                EvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                ),
                PolicyTestFixtures.DECISION_AT.minus(Duration.ofHours(2)),
                PolicyTestFixtures.DECISION_AT.minus(Duration.ofHours(2))
        );

        assertEquals(
                EvidenceTemporalDecision.STALE,
                policy.decide(
                        stale,
                        EvidenceCategory.BANKING_VERIFICATION,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.temporalProfile()
                ).decision()
        );
    }

    @Test
    void authorizationPolicyAcceptsApprovedBoundEvidence() {
        AuthorizationEvidenceSnapshot evidence =
                new AuthorizationEvidenceSnapshot(
                        new AuthorizationEvidenceReference(
                                "v1:hmac-sha256:" + "a".repeat(64)
                        ),
                        AuthorizationDecisionOutcome.APPROVED,
                        EvidenceFingerprint.of(
                                "v1:sha256:" + "b".repeat(64)
                        ),
                        "issuer",
                        "key-01",
                        "RS256",
                        "payment:initiate",
                        List.of(
                                new AuthorizationBindingEvidence(
                                        AuthorizationBindingType.PAYMENT_SCOPE,
                                        AuthorizationBindingResult.MATCH
                                ),
                                new AuthorizationBindingEvidence(
                                        AuthorizationBindingType.DEBTOR_ACCOUNT,
                                        AuthorizationBindingResult.MATCH
                                )
                        ),
                        PolicyTestFixtures.DECISION_AT.minusSeconds(600),
                        PolicyTestFixtures.DECISION_AT.minusSeconds(600),
                        PolicyTestFixtures.DECISION_AT.plusSeconds(600),
                        null,
                        new EvidenceMetadata(
                                ExternalSystem.TRESOR_PAY,
                                com.sixpay.common.context.CorrelationId.of(
                                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                                ),
                                EvidenceObservationChannel.LOCAL_VALIDATION,
                                EvidenceFingerprint.of(
                                        "v1:sha256:" + "c".repeat(64)
                                ),
                                PolicyTestFixtures.DECISION_AT.minusSeconds(5),
                                PolicyTestFixtures.DECISION_AT.minusSeconds(4)
                        )
                );

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                new AuthorizationEvidenceAcceptancePolicy().decide(
                        new PaymentAuthorizationContext(
                                ExternalSubscriptionReference.of("SUB-001"),
                                ExternalPaymentReference.of("PAYMENT-001"),
                                FinancialInstitutionCode.of("BANK_CM"),
                                "v1:" + "d".repeat(64)
                        ),
                        evidence,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.authorizationProfile()
                );

        assertEquals(
                EvidenceAcceptanceDecision.ACCEPT,
                decision.decision()
        );
    }

    @Test
    void bankingAndFundsPoliciesRejectBindingConflicts() {
        BankingVerificationSnapshot banking =
                new BankingVerificationSnapshot(
                        new BankingVerificationId(UUID.randomUUID()),
                        BankingVerificationOutcome.VERIFIED,
                        "v1:" + "a".repeat(64),
                        List.of(
                                new BankingVerificationCheckEvidence(
                                        BankingVerificationCheckType
                                                .CUSTOMER_EXISTS,
                                        EvidenceCheckResult.PASS,
                                        null,
                                        null
                                ),
                                new BankingVerificationCheckEvidence(
                                        BankingVerificationCheckType
                                                .ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS,
                                        null,
                                        null
                                )
                        ),
                        PolicyTestFixtures.evidenceMetadata(
                                EvidenceObservationChannel.DIRECT_RESPONSE
                        )
                );

        assertEquals(
                EvidenceAcceptanceDecision.CONFLICT,
                new BankingVerificationAcceptancePolicy().decide(
                        new PaymentBankingContext(
                                FinancialInstitutionCode.of("BANK_CM"),
                                "v1:" + "b".repeat(64)
                        ),
                        banking,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.bankingProfile()
                ).decision()
        );

        FundsControlSnapshot funds = new FundsControlSnapshot(
                new FundsVerificationReference("FUNDS-RESULT-001"),
                FundsControlOutcome.VERIFIED,
                PolicyTestFixtures.amount(),
                "v1:" + "a".repeat(64),
                List.of(
                        new FundsControlCheckEvidence(
                                FundsControlCheckType.ACCOUNT_EXISTS,
                                EvidenceCheckResult.PASS,
                                null,
                                PolicyTestFixtures.DECISION_AT.minusSeconds(5)
                        ),
                        new FundsControlCheckEvidence(
                                FundsControlCheckType
                                        .AVAILABLE_FUNDS_SUFFICIENT,
                                EvidenceCheckResult.PASS,
                                null,
                                PolicyTestFixtures.DECISION_AT.minusSeconds(5)
                        )
                ),
                PolicyTestFixtures.DECISION_AT.plusSeconds(60),
                PolicyTestFixtures.evidenceMetadata(
                        EvidenceObservationChannel.DIRECT_RESPONSE
                )
        );

        assertEquals(
                EvidenceAcceptanceDecision.CONFLICT,
                new FundsControlAcceptancePolicy().decide(
                        new PaymentFundsContext(
                                FinancialInstitutionCode.of("BANK_CM"),
                                "v1:" + "b".repeat(64),
                                PolicyTestFixtures.amount()
                        ),
                        funds,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.fundsProfile()
                ).decision()
        );
    }
}

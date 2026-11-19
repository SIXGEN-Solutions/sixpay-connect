package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.payment.domain.model.evidence.TreasuryAccountResolutionSnapshot;
import com.sixpay.payment.domain.model.evidence.TreasuryResolutionOutcome;
import com.sixpay.payment.domain.policy.EvidenceAcceptanceDecision;
import com.sixpay.payment.domain.policy.PaymentTreasuryContext;
import com.sixpay.payment.domain.policy.PolicyDecision;
import com.sixpay.payment.domain.policy.PolicyProfileMetadata;
import com.sixpay.payment.domain.policy.TreasuryResolutionAcceptancePolicy;
import com.sixpay.payment.domain.policy.TreasuryResolutionPolicyProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TreasuryAccountReferenceIdentityTest {

    private static final FinancialInstitutionCode INSTITUTION =
            FinancialInstitutionCode.of("REGIONAL");
    private static final String CONFIG_ID = "TREASURY-BANK-CM-AVI";
    private static final String ACCOUNT_A = "00001-12345671051-01";
    private static final String ACCOUNT_B = "00001-98765431051-01";
    private static final Instant WHEN = Instant.parse("2026-11-14T12:00:00Z");
    private static final EvidenceFingerprint FINGERPRINT =
            EvidenceFingerprint.of("v1:sha256:" + "a".repeat(64));

    @Test
    void sameBusinessIdentityHasEqualHashRegardlessOfMaskedDisplay() {
        TreasuryAccountReference first = ref(ACCOUNT_A, "v1", "****1051");
        TreasuryAccountReference same = ref(ACCOUNT_A, "v1", "****xxxx");
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
    }

    @Test
    void accountDifferenceMustNotBeMaskedBySameConfigurationAndVersion() {
        TreasuryAccountReference accountA = ref(ACCOUNT_A, "v1", "****1051");
        TreasuryAccountReference accountB = ref(ACCOUNT_B, "v1", "****1051");
        assertNotEquals(accountA, accountB);
        assertNotEquals(accountB, accountA);
    }

    @Test
    void configurationVersionDifferenceChangesIdentity() {
        assertNotEquals(ref(ACCOUNT_A, "v1", "****1051"),
                ref(ACCOUNT_A, "v2", "****1051"));
    }

    @Test
    void resolvedEvidenceWithDifferentAccountsIsNotEqual() {
        assertNotEquals(
                resolvedEvidence(ref(ACCOUNT_A, "v1", "****1051")),
                resolvedEvidence(ref(ACCOUNT_B, "v1", "****1051"))
        );
    }

    @Test
    void treasuryPolicyRejectsSubstitutionWithinSameConfigurationVersion() {
        TreasuryAccountReference persisted = ref(ACCOUNT_A, "v1", "****1051");
        TreasuryAccountReference candidate = ref(ACCOUNT_B, "v1", "****1051");

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                new TreasuryResolutionAcceptancePolicy().decide(
                        new PaymentTreasuryContext(INSTITUTION, FINGERPRINT),
                        resolvedEvidence(candidate),
                        persisted,
                        WHEN,
                        approvedProfile()
                );
        assertEquals(EvidenceAcceptanceDecision.CONFLICT, decision.decision());
        assertEquals("TREASURY_REFERENCE_CONFLICT", decision.reasonCode());
    }

    @Test
    void treasuryPolicyAcceptsUnchangedResolvedAccount() {
        TreasuryAccountReference persisted = ref(ACCOUNT_A, "v1", "****1051");

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                new TreasuryResolutionAcceptancePolicy().decide(
                        new PaymentTreasuryContext(INSTITUTION, FINGERPRINT),
                        resolvedEvidence(ref(ACCOUNT_A, "v1", "****xxxx")),
                        persisted,
                        WHEN,
                        approvedProfile()
                );
        assertEquals(EvidenceAcceptanceDecision.ACCEPT, decision.decision());
        assertEquals("TREASURY_RESOLUTION_ACCEPTED", decision.reasonCode());
    }

    private static TreasuryAccountReference ref(String account, String version, String mask) {
        return new TreasuryAccountReference(INSTITUTION, CONFIG_ID, account, mask, version);
    }

    private static TreasuryAccountResolutionSnapshot resolvedEvidence(
            TreasuryAccountReference reference
    ) {
        EvidenceMetadata metadata = new EvidenceMetadata(
                ExternalSystem.SIXPAY,
                CorrelationId.of("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                EvidenceObservationChannel.PROTECTED_CONFIGURATION_RESOLUTION,
                FINGERPRINT,
                WHEN,
                WHEN
        );
        return new TreasuryAccountResolutionSnapshot(
                reference, FINGERPRINT, TreasuryResolutionOutcome.RESOLVED,
                "v1", null, metadata
        );
    }

    private static TreasuryResolutionPolicyProfile approvedProfile() {
        return new TreasuryResolutionPolicyProfile(
                new PolicyProfileMetadata("TREASURY-MVP", "v1",
                        WHEN.minusSeconds(3600), null, "APPROVED-TEST"),
                Map.of(INSTITUTION, Set.of("v1"))
        );
    }
}

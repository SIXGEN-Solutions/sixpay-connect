package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.TreasuryAccountResolutionSnapshot;
import com.sixpay.payment.domain.model.evidence.TreasuryResolutionOutcome;

import java.time.Instant;
import java.util.Objects;

public final class TreasuryResolutionAcceptancePolicy {

    public PolicyDecision<EvidenceAcceptanceDecision> decide(
            PaymentTreasuryContext context,
            TreasuryAccountResolutionSnapshot evidence,
            TreasuryAccountReference currentReference,
            Instant decisionAt,
            TreasuryResolutionPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Treasury context");
        Objects.requireNonNull(evidence, "Treasury evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Treasury profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)) {
            return result(profile, EvidenceAcceptanceDecision.REJECT,
                    "PROFILE_NOT_EFFECTIVE");
        }

        if (!context.allocationIntentFingerprint().equals(
                evidence.allocationIntentFingerprint()
        )) {
            return result(profile, EvidenceAcceptanceDecision.CONFLICT,
                    "ALLOCATION_FINGERPRINT_CONFLICT");
        }

        if (evidence.resolutionOutcome()
                == TreasuryResolutionOutcome.REJECTED) {
            return result(profile, EvidenceAcceptanceDecision.REJECT,
                    "TREASURY_RESOLUTION_REJECTED");
        }

        TreasuryAccountReference resolved =
                evidence.treasuryAccountReference().orElseThrow();

        if (!context.financialInstitutionCode().equals(
                resolved.financialInstitutionCode()
        ) || !profile.isApproved(
                resolved.financialInstitutionCode(),
                resolved.configurationVersion()
        )) {
            return result(profile, EvidenceAcceptanceDecision.REJECT,
                    "TREASURY_CONFIGURATION_NOT_APPROVED");
        }

        if (currentReference != null
                && !currentReference.equals(resolved)) {
            return result(profile, EvidenceAcceptanceDecision.CONFLICT,
                    "TREASURY_REFERENCE_CONFLICT");
        }

        return result(profile, EvidenceAcceptanceDecision.ACCEPT,
                "TREASURY_RESOLUTION_ACCEPTED");
    }

    private static PolicyDecision<EvidenceAcceptanceDecision> result(
            TreasuryResolutionPolicyProfile profile,
            EvidenceAcceptanceDecision decision,
            String reason
    ) {
        return PolicyDecision.withProfile(
                decision,
                reason,
                profile.metadata()
        );
    }
}

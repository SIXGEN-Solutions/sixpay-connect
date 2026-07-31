package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.policy.*;

import java.util.Objects;

public record PaymentPolicyBundle(
        EvidenceTemporalProfile evidenceTemporalProfile,
        AuthorizationPolicyProfile authorizationPolicyProfile,
        BankingVerificationPolicyProfile bankingVerificationPolicyProfile,
        FundsControlPolicyProfile fundsControlPolicyProfile,
        TreasuryResolutionPolicyProfile treasuryResolutionPolicyProfile,
        PostingAuthorizationPolicyProfile postingAuthorizationPolicyProfile,
        FinancialOutcomePolicyProfile financialOutcomePolicyProfile,
        TfjPolicyProfile tfjPolicyProfile,
        ReversalPolicyProfile reversalPolicyProfile,
        FailureClassificationProfile failureClassificationProfile,
        ResultIntentPolicyProfile resultIntentPolicyProfile,
        EventDisclosureProfile eventDisclosureProfile
) {
    public PaymentPolicyBundle {
        evidenceTemporalProfile = Objects.requireNonNull(
                evidenceTemporalProfile,
                "Evidence temporal profile"
        );
        authorizationPolicyProfile = Objects.requireNonNull(
                authorizationPolicyProfile,
                "Authorization policy profile"
        );
        bankingVerificationPolicyProfile = Objects.requireNonNull(
                bankingVerificationPolicyProfile,
                "Banking policy profile"
        );
        fundsControlPolicyProfile = Objects.requireNonNull(
                fundsControlPolicyProfile,
                "Funds-control policy profile"
        );
        treasuryResolutionPolicyProfile = Objects.requireNonNull(
                treasuryResolutionPolicyProfile,
                "Treasury policy profile"
        );
        postingAuthorizationPolicyProfile = Objects.requireNonNull(
                postingAuthorizationPolicyProfile,
                "Posting authorization profile"
        );
        financialOutcomePolicyProfile = Objects.requireNonNull(
                financialOutcomePolicyProfile,
                "Financial outcome profile"
        );
        tfjPolicyProfile = Objects.requireNonNull(
                tfjPolicyProfile,
                "TFJ profile"
        );
        reversalPolicyProfile = Objects.requireNonNull(
                reversalPolicyProfile,
                "Reversal profile"
        );
        failureClassificationProfile = Objects.requireNonNull(
                failureClassificationProfile,
                "Failure classification profile"
        );
        resultIntentPolicyProfile = Objects.requireNonNull(
                resultIntentPolicyProfile,
                "Result-intent profile"
        );
        eventDisclosureProfile = Objects.requireNonNull(
                eventDisclosureProfile,
                "Event disclosure profile"
        );
    }
}

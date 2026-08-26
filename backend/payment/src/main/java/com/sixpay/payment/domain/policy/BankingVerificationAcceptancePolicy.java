package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class BankingVerificationAcceptancePolicy {

    private final EvidenceTemporalValidityPolicy temporalPolicy =
            new EvidenceTemporalValidityPolicy();

    public PolicyDecision<EvidenceAcceptanceDecision> decide(
            PaymentBankingContext context,
            BankingVerificationSnapshot evidence,
            Instant decisionAt,
            BankingVerificationPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Banking context");
        Objects.requireNonNull(evidence, "Banking evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Banking profile");

        if (!context.accountBindingFingerprint().equals(
                evidence.accountBindingFingerprint()
        )) {
            return result(profile, EvidenceAcceptanceDecision.CONFLICT,
                    "ACCOUNT_BINDING_CONFLICT");
        }

        PolicyDecision<EvidenceTemporalDecision> temporal =
                temporalPolicy.decide(
                        evidence.metadata(),
                        EvidenceCategory.BANKING_VERIFICATION,
                        decisionAt,
                        profile.temporalProfile()
                );

        if (temporal.decision() != EvidenceTemporalDecision.VALID) {
            return result(profile, EvidenceAcceptanceDecision.INDETERMINATE,
                    temporal.reasonCode());
        }

        Set<?> passed = evidence.checks().stream()
                .filter(check ->
                        check.result() == EvidenceCheckResult.PASS
                )
                .map(check -> check.type())
                .collect(Collectors.toSet());

        if (!passed.containsAll(profile.mandatoryChecks())) {
            return result(profile, EvidenceAcceptanceDecision.INDETERMINATE,
                    "MANDATORY_BANKING_CHECK_NOT_PASSED");
        }

        return switch (evidence.outcome()) {
            case VERIFIED -> result(
                    profile,
                    EvidenceAcceptanceDecision.ACCEPT,
                    "BANKING_VERIFICATION_ACCEPTED"
            );
            case REJECTED -> result(
                    profile,
                    EvidenceAcceptanceDecision.REJECT,
                    "BANKING_VERIFICATION_REJECTED"
            );
            case INDETERMINATE -> result(
                    profile,
                    EvidenceAcceptanceDecision.INDETERMINATE,
                    "BANKING_VERIFICATION_INDETERMINATE"
            );
        };
    }

    private static PolicyDecision<EvidenceAcceptanceDecision> result(
            BankingVerificationPolicyProfile profile,
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

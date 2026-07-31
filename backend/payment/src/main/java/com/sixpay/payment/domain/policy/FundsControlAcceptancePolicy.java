package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.FundsControlOutcome;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class FundsControlAcceptancePolicy {

    private final EvidenceTemporalValidityPolicy temporalPolicy =
            new EvidenceTemporalValidityPolicy();

    public PolicyDecision<EvidenceAcceptanceDecision> decide(
            PaymentFundsContext context,
            FundsControlSnapshot evidence,
            Instant decisionAt,
            FundsControlPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Funds context");
        Objects.requireNonNull(evidence, "Funds evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Funds profile");

        if (!context.accountBindingFingerprint().equals(
                evidence.accountBindingFingerprint()
        ) || !context.amount().equals(evidence.checkedAmount())) {
            return result(profile, EvidenceAcceptanceDecision.CONFLICT,
                    "FUNDS_BINDING_CONFLICT");
        }

        if (decisionAt.isAfter(evidence.validUntil())) {
            return result(profile, EvidenceAcceptanceDecision.INDETERMINATE,
                    "FUNDS_EVIDENCE_EXPIRED");
        }

        PolicyDecision<EvidenceTemporalDecision> temporal =
                temporalPolicy.decide(
                        evidence.metadata(),
                        EvidenceCategory.FUNDS_CONTROL,
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
                    "MANDATORY_FUNDS_CHECK_NOT_PASSED");
        }

        return switch (evidence.outcome()) {
            case VERIFIED -> result(
                    profile,
                    EvidenceAcceptanceDecision.ACCEPT,
                    "FUNDS_CONTROL_ACCEPTED"
            );
            case REJECTED -> result(
                    profile,
                    EvidenceAcceptanceDecision.REJECT,
                    "FUNDS_CONTROL_REJECTED"
            );
            case INDETERMINATE -> result(
                    profile,
                    EvidenceAcceptanceDecision.INDETERMINATE,
                    "FUNDS_CONTROL_INDETERMINATE"
            );
        };
    }

    private static PolicyDecision<EvidenceAcceptanceDecision> result(
            FundsControlPolicyProfile profile,
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

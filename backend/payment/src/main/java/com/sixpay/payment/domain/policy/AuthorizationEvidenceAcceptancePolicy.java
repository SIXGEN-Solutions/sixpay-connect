package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.AuthorizationBindingResult;
import com.sixpay.payment.domain.model.evidence.AuthorizationDecisionOutcome;
import com.sixpay.payment.domain.model.evidence.AuthorizationEvidenceSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthorizationEvidenceAcceptancePolicy {

    private final EvidenceTemporalValidityPolicy temporalPolicy =
            new EvidenceTemporalValidityPolicy();

    public PolicyDecision<EvidenceAcceptanceDecision> decide(
            PaymentAuthorizationContext context,
            AuthorizationEvidenceSnapshot evidence,
            Instant decisionAt,
            AuthorizationPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Authorization context");
        Objects.requireNonNull(evidence, "Authorization evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Authorization profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)) {
            return reject(profile, "PROFILE_NOT_EFFECTIVE");
        }

        if (!profile.allowedIssuers().contains(evidence.issuer())
                || !profile.allowedAlgorithms().contains(
                        evidence.signatureAlgorithm()
                )
                || !profile.allowedScopes().contains(evidence.scope())) {
            return reject(profile, "AUTHORIZATION_PROFILE_MISMATCH");
        }

        Set<?> evaluated = evidence.bindingResults().stream()
                .filter(binding ->
                        binding.result()
                                == AuthorizationBindingResult.MATCH
                )
                .map(binding -> binding.type())
                .collect(Collectors.toSet());

        if (!evaluated.containsAll(profile.mandatoryBindings())) {
            return PolicyDecision.withProfile(
                    EvidenceAcceptanceDecision.INDETERMINATE,
                    "MANDATORY_BINDING_NOT_MATCHED",
                    profile.metadata()
            );
        }

        if (evidence.outcome()
                == AuthorizationDecisionOutcome.REJECTED) {
            return reject(profile, "AUTHORIZATION_REJECTED");
        }

        if (decisionAt.isBefore(evidence.validFrom())
                || decisionAt.isAfter(evidence.expiresAt())) {
            return reject(profile, "TOKEN_OUTSIDE_VALIDITY_WINDOW");
        }

        return PolicyDecision.withProfile(
                EvidenceAcceptanceDecision.ACCEPT,
                "AUTHORIZATION_ACCEPTED",
                profile.metadata()
        );
    }

    private static PolicyDecision<EvidenceAcceptanceDecision> reject(
            AuthorizationPolicyProfile profile,
            String reason
    ) {
        return PolicyDecision.withProfile(
                EvidenceAcceptanceDecision.REJECT,
                reason,
                profile.metadata()
        );
    }
}

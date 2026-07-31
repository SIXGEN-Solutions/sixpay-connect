package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationEvidence;

import java.time.Instant;
import java.util.Objects;

public final class ReversalAuthorizationPolicy {

    public PolicyDecision<ReversalAuthorizationDecision> decide(
            PaymentReversalEligibilityContext context,
            ReversalInstructionIdentity candidate,
            ReversalAuthorizationEvidence authorization,
            Instant decisionAt,
            ReversalPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Reversal eligibility context");
        Objects.requireNonNull(candidate, "Reversal instruction identity");
        Objects.requireNonNull(authorization, "Reversal authorization");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Reversal profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)
                || !profile.acceptedAuthorizationTypes().contains(
                        authorization.authorizationType()
                )
                || !profile.acceptedReasonCodes().contains(
                        authorization.reasonCode().value()
                )) {
            return result(
                    profile,
                    ReversalAuthorizationDecision.REJECT_INELIGIBLE,
                    "REVERSAL_AUTHORIZATION_NOT_APPROVED"
            );
        }

        if (context.currentInstructionId() != null) {
            if (context.currentInstructionId().equals(
                    candidate.instructionId()
            )) {
                return result(
                        profile,
                        ReversalAuthorizationDecision
                                .NO_OP_SAME_INSTRUCTION,
                        "SAME_REVERSAL_INSTRUCTION"
                );
            }
            return result(
                    profile,
                    ReversalAuthorizationDecision
                            .REJECT_SECOND_OR_CONFLICTING_INSTRUCTION,
                    "SECOND_REVERSAL_INSTRUCTION_FORBIDDEN"
            );
        }

        if (context.financialEffectKnowledge()
                == FinancialEffectKnowledge.PROVEN_NONE
                || context.financialEffectKnowledge()
                == FinancialEffectKnowledge.UNCERTAIN
                || context.financialEffectKnowledge()
                == FinancialEffectKnowledge.REVERSED) {
            return result(
                    profile,
                    ReversalAuthorizationDecision.REJECT_INELIGIBLE,
                    "NO_REVERSIBLE_CONFIRMED_EFFECT"
            );
        }

        return result(
                profile,
                ReversalAuthorizationDecision.AUTHORIZE,
                "REVERSAL_AUTHORIZED"
        );
    }

    private static PolicyDecision<ReversalAuthorizationDecision> result(
            ReversalPolicyProfile profile,
            ReversalAuthorizationDecision decision,
            String reason
    ) {
        return PolicyDecision.withProfile(
                decision,
                reason,
                profile.metadata()
        );
    }
}

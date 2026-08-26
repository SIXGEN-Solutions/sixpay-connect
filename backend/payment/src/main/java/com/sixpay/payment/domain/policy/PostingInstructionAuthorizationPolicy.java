package com.sixpay.payment.domain.policy;

import java.time.Instant;
import java.util.Objects;

public final class PostingInstructionAuthorizationPolicy {

    public PolicyDecision<PostingAuthorizationDecision> decide(
            PaymentPostingAuthorizationContext context,
            PostingInstructionIdentity candidate,
            Instant decisionAt,
            PostingAuthorizationPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Posting authorization context");
        Objects.requireNonNull(candidate, "Posting instruction identity");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "Posting authorization profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)
                || !profile.eligibleStatuses().contains(context.status())) {
            return result(
                    profile,
                    PostingAuthorizationDecision.REJECT_INELIGIBLE,
                    "POSTING_STATUS_OR_PROFILE_INELIGIBLE"
            );
        }

        if (context.hasInstruction()) {
            if (context.currentInstructionId().equals(
                    candidate.instructionId()
            )) {
                return result(
                        profile,
                        PostingAuthorizationDecision
                                .NO_OP_SAME_INSTRUCTION,
                        "SAME_POSTING_INSTRUCTION"
                );
            }
            return result(
                    profile,
                    PostingAuthorizationDecision
                            .REJECT_SECOND_OR_CONFLICTING_INSTRUCTION,
                    "SECOND_POSTING_INSTRUCTION_FORBIDDEN"
            );
        }

        if (!context.authorizationAccepted()
                || !context.bankingVerified()
                || !context.fundsVerified()
                || (profile.requireFreshFundsEvidence()
                        && !context.fundsFresh())
                || (profile.requireResolvedTreasuryAccount()
                        && !context.treasuryResolved())) {
            return result(
                    profile,
                    PostingAuthorizationDecision.REJECT_INELIGIBLE,
                    "REQUIRED_POSTING_EVIDENCE_NOT_SATISFIED"
            );
        }

        return result(
                profile,
                PostingAuthorizationDecision.AUTHORIZE,
                "POSTING_AUTHORIZED"
        );
    }

    private static PolicyDecision<PostingAuthorizationDecision> result(
            PostingAuthorizationPolicyProfile profile,
            PostingAuthorizationDecision decision,
            String reason
    ) {
        return PolicyDecision.withProfile(
                decision,
                reason,
                profile.metadata()
        );
    }
}

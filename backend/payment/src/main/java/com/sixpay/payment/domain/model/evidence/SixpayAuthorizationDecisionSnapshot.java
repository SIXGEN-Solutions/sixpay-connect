package com.sixpay.payment.domain.model.evidence;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable Payment-owned record of the local SIXPAY authorization decision.
 *
 * <p>This snapshot is distinct from {@link AuthorizationEvidenceSnapshot},
 * which represents the legacy/external authorization-evidence path.</p>
 */
public record SixpayAuthorizationDecisionSnapshot(
        AuthorizationDecisionOutcome outcome,
        Instant decidedAt
) {

    public SixpayAuthorizationDecisionSnapshot {
        Objects.requireNonNull(outcome, "Authorization decision outcome");
        Objects.requireNonNull(decidedAt, "Authorization decision instant");
    }

    public boolean approved() {
        return outcome == AuthorizationDecisionOutcome.APPROVED;
    }
}

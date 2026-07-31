package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted a canonical authorization decision.
 */
public record PaymentAuthorizationDecisionRecorded(
        PaymentEventMetadata metadata,
        AuthorizationDecisionOutcome outcome,
        AuthorizationEvidenceReference authorizationEvidenceReference,
        EvidenceFingerprint evidenceFingerprint,
        FailureCode rejectionCode,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentAuthorizationDecisionRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        outcome = Objects.requireNonNull(outcome, "outcome");
        authorizationEvidenceReference = Objects.requireNonNull(authorizationEvidenceReference, "authorizationEvidenceReference");
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
    }
}

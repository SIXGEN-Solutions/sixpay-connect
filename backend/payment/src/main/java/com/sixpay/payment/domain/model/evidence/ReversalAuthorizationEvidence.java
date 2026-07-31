package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;

public record ReversalAuthorizationEvidence(
        ReversalAuthorizationType authorizationType,
        ReversalAuthorizationReference authorizationReference,
        String requestedBySubject,
        FailureCode reasonCode,
        Instant authorizedAt,
        Instant requestedAt
) implements ValueObject {

    public ReversalAuthorizationEvidence {
        authorizationType = EvidenceValueObjectRules.requireNonNull(
                authorizationType,
                "Reversal authorization type"
        );
        authorizationReference = EvidenceValueObjectRules.requireNonNull(
                authorizationReference,
                "Reversal authorization reference"
        );
        requestedBySubject = EvidenceValueObjectRules.requireOpaque(
                requestedBySubject,
                1,
                128,
                "Reversal requested-by subject"
        );
        reasonCode = EvidenceValueObjectRules.requireNonNull(
                reasonCode,
                "Reversal reason code"
        );
        authorizedAt = EvidenceValueObjectRules.requireNonNull(
                authorizedAt,
                "Reversal authorization instant"
        );
        requestedAt = EvidenceValueObjectRules.requireNonNull(
                requestedAt,
                "Reversal request instant"
        );

        EvidenceValueObjectRules.requireNotBefore(
                requestedAt,
                authorizedAt,
                "Reversal request must not precede authorization"
        );
    }

    @Override
    public String toString() {
        return "ReversalAuthorizationEvidence[type="
                + authorizationType
                + ", reference=" + authorizationReference
                + ", reasonCode=" + reasonCode
                + ", authorizedAt=" + authorizedAt
                + ", requestedAt=" + requestedAt + "]";
    }
}

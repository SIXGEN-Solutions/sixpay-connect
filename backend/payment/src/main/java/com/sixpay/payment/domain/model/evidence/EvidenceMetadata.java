package com.sixpay.payment.domain.model.evidence;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;

public record EvidenceMetadata(
        ExternalSystem sourceSystem,
        CorrelationId correlationId,
        EvidenceObservationChannel observationChannel,
        EvidenceFingerprint evidenceFingerprint,
        Instant observedAt,
        Instant acceptedAt
) implements ValueObject {

    public EvidenceMetadata {
        sourceSystem = EvidenceValueObjectRules.requireSnapshotSource(sourceSystem);
        EvidenceValueObjectRules.requireCanonicalCorrelationId(correlationId);
        observationChannel = EvidenceValueObjectRules.requireNonNull(
                observationChannel,
                "Evidence observation channel"
        );
        evidenceFingerprint = EvidenceValueObjectRules.requireNonNull(
                evidenceFingerprint,
                "Evidence fingerprint"
        );
        observedAt = EvidenceValueObjectRules.requireNonNull(
                observedAt,
                "Evidence observation instant"
        );
        acceptedAt = EvidenceValueObjectRules.requireNonNull(
                acceptedAt,
                "Evidence acceptance instant"
        );
        EvidenceValueObjectRules.requireNotBefore(
                acceptedAt,
                observedAt,
                "Evidence acceptance instant must not precede observation"
        );
    }

    @Override
    public String toString() {
        return "EvidenceMetadata[sourceSystem="
                + sourceSystem
                + ", observationChannel=" + observationChannel
                + ", observedAt=" + observedAt
                + ", acceptedAt=" + acceptedAt + "]";
    }
}

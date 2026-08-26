package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable proof that the bank accepted the customer's confirmation.
 *
 * <p>The evidence stores only opaque references and fingerprints. It never
 * stores the OTP, PIN or any authentication secret.</p>
 */
public record CustomerConfirmationEvidence(
        CustomerConfirmationReference confirmationReference,
        EvidenceFingerprint confirmationFingerprint,
        Instant confirmedAt,
        EvidenceMetadata metadata
) implements ValueObject {

    public CustomerConfirmationEvidence {
        confirmationReference = Objects.requireNonNull(
                confirmationReference,
                "Customer confirmation reference"
        );
        confirmationFingerprint = Objects.requireNonNull(
                confirmationFingerprint,
                "Customer confirmation fingerprint"
        );
        confirmedAt = Objects.requireNonNull(
                confirmedAt,
                "Customer confirmation instant"
        );
        metadata = Objects.requireNonNull(
                metadata,
                "Customer confirmation metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE) {
            throw new IllegalArgumentException(
                    "Customer confirmation evidence source must be AMPLITUDE"
            );
        }
        if (!metadata.evidenceFingerprint().equals(
                confirmationFingerprint
        )) {
            throw new IllegalArgumentException(
                    "Customer confirmation fingerprints must match"
            );
        }
        if (confirmedAt.isBefore(metadata.observedAt())
                || confirmedAt.isAfter(metadata.acceptedAt())) {
            throw new IllegalArgumentException(
                    "Customer confirmation instant must be within evidence observation"
            );
        }
    }

    @Override
    public String toString() {
        return "CustomerConfirmationEvidence[reference="
                + confirmationReference
                + ", confirmedAt=" + confirmedAt
                + ", metadata=" + metadata + "]";
    }
}

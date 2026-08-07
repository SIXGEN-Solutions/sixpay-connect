package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

public final class FundsReleaseSnapshot implements ValueObject {

    private final FundsReservationReference reservationReference;
    private final FundsReleaseOutcome outcome;
    private final String releaseReference;
    private final FailureCode reasonCode;
    private final EvidenceMetadata metadata;

    public FundsReleaseSnapshot(
            FundsReservationReference reservationReference,
            FundsReleaseOutcome outcome,
            String releaseReference,
            FailureCode reasonCode,
            EvidenceMetadata metadata
    ) {
        this.reservationReference =
                EvidenceValueObjectRules.requireNonNull(
                        reservationReference,
                        "Reservation reference"
                );
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Release outcome"
        );
        this.releaseReference = releaseReference == null
                ? null
                : EvidenceValueObjectRules
                        .requirePrintableAsciiNoWhitespace(
                                releaseReference,
                                1,
                                128,
                                "Release reference"
                        );
        this.reasonCode = reasonCode;
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Release metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE) {
            throw new IllegalArgumentException(
                    "Release source must be AMPLITUDE"
            );
        }

        switch (outcome) {
            case RELEASED, ALREADY_RELEASED -> {
                if (this.releaseReference == null || reasonCode != null) {
                    throw new IllegalArgumentException(
                            "Successful release requires a reference and no failure code"
                    );
                }
            }
            case REJECTED -> {
                if (reasonCode == null) {
                    throw new IllegalArgumentException(
                            "Rejected release requires a failure code"
                    );
                }
            }
            case UNKNOWN -> {
                if (reasonCode != null) {
                    throw new IllegalArgumentException(
                            "Unknown release must not claim a conclusive reason"
                    );
                }
            }
        }
    }

    public FundsReservationReference reservationReference() {
        return reservationReference;
    }

    public FundsReleaseOutcome outcome() {
        return outcome;
    }

    public Optional<String> releaseReference() {
        return Optional.ofNullable(releaseReference);
    }

    public Optional<FailureCode> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FundsReleaseSnapshot that)) return false;
        return reservationReference.equals(that.reservationReference)
                && outcome == that.outcome
                && Objects.equals(releaseReference, that.releaseReference)
                && Objects.equals(reasonCode, that.reasonCode)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                reservationReference,
                outcome,
                releaseReference,
                reasonCode,
                metadata
        );
    }
}

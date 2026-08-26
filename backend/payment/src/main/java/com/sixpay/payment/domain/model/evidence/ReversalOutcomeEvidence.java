package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ReversalOutcomeEvidence implements ValueObject {

    private static final Set<EvidenceObservationChannel> ALLOWED_CHANNELS =
            Set.of(
                    EvidenceObservationChannel.DIRECT_RESPONSE,
                    EvidenceObservationChannel.BANK_REFERENCE_LOOKUP
            );

    private final ReversalReference reversalReference;
    private final ReversalOutcome outcome;
    private final String reversalEntryReference;
    private final FailureCode reasonCode;
    private final EvidenceMetadata metadata;

    public ReversalOutcomeEvidence(
            ReversalReference reversalReference,
            ReversalOutcome outcome,
            String reversalEntryReference,
            FailureCode reasonCode,
            EvidenceMetadata metadata
    ) {
        this.reversalReference = reversalReference;
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Reversal outcome"
        );
        this.reversalEntryReference = reversalEntryReference == null
                ? null
                : EvidenceValueObjectRules
                        .requirePrintableAsciiNoWhitespace(
                                reversalEntryReference,
                                1,
                                128,
                                "Reversal entry reference"
                        );
        this.reasonCode = reasonCode;
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Reversal outcome metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE
                || !ALLOWED_CHANNELS.contains(
                        metadata.observationChannel()
                )) {
            throw new IllegalArgumentException(
                    "Reversal outcome source or channel is invalid"
            );
        }

        switch (outcome) {
            case REVERSED -> {
                if (reversalReference == null || reasonCode != null) {
                    throw new IllegalArgumentException(
                            "Reversed outcome requires a reference and no reason code"
                    );
                }
            }
            case REJECTED, NOT_ALLOWED -> {
                if (reasonCode == null) {
                    throw new IllegalArgumentException(
                            "Rejected or not-allowed reversal requires a reason code"
                    );
                }
            }
            case UNKNOWN -> {
                if (reasonCode != null) {
                    throw new IllegalArgumentException(
                            "Unknown reversal must not claim a conclusive reason"
                    );
                }
            }
        }
    }

    public Optional<ReversalReference> reversalReference() {
        return Optional.ofNullable(reversalReference);
    }

    public ReversalOutcome outcome() {
        return outcome;
    }

    public Optional<String> reversalEntryReference() {
        return Optional.ofNullable(reversalEntryReference);
    }

    public Optional<FailureCode> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReversalOutcomeEvidence that)) {
            return false;
        }
        return Objects.equals(
                reversalReference,
                that.reversalReference
        ) && outcome == that.outcome
                && Objects.equals(
                        reversalEntryReference,
                        that.reversalEntryReference
                )
                && Objects.equals(reasonCode, that.reasonCode)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                reversalReference,
                outcome,
                reversalEntryReference,
                reasonCode,
                metadata
        );
    }

    @Override
    public String toString() {
        return "ReversalOutcomeEvidence[outcome="
                + outcome
                + ", reversalReference=" + reversalReference
                + ", entryReference=" + reversalEntryReference
                + ", metadata=" + metadata + "]";
    }
}

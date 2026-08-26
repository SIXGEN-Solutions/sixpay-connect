package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

public final class TreasuryAccountResolutionSnapshot
        implements ValueObject {

    private final TreasuryAccountReference treasuryAccountReference;
    private final EvidenceFingerprint allocationIntentFingerprint;
    private final TreasuryResolutionOutcome resolutionOutcome;
    private final String resolverPolicyVersion;
    private final FailureCode rejectionCode;
    private final EvidenceMetadata metadata;

    public TreasuryAccountResolutionSnapshot(
            TreasuryAccountReference treasuryAccountReference,
            EvidenceFingerprint allocationIntentFingerprint,
            TreasuryResolutionOutcome resolutionOutcome,
            String resolverPolicyVersion,
            FailureCode rejectionCode,
            EvidenceMetadata metadata
    ) {
        this.treasuryAccountReference = treasuryAccountReference;
        this.allocationIntentFingerprint =
                EvidenceValueObjectRules.requireNonNull(
                        allocationIntentFingerprint,
                        "Allocation intent fingerprint"
                );
        this.resolutionOutcome = EvidenceValueObjectRules.requireNonNull(
                resolutionOutcome,
                "Treasury resolution outcome"
        );
        this.resolverPolicyVersion = EvidenceValueObjectRules.requireOpaque(
                resolverPolicyVersion,
                1,
                128,
                "Treasury resolver policy version"
        );
        this.rejectionCode = rejectionCode;
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Treasury resolution metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.SIXPAY
                || metadata.observationChannel()
                != EvidenceObservationChannel
                        .PROTECTED_CONFIGURATION_RESOLUTION) {
            throw new IllegalArgumentException(
                    "Treasury resolution must come from SIXPAY protected configuration"
            );
        }

        if (resolutionOutcome == TreasuryResolutionOutcome.RESOLVED) {
            if (treasuryAccountReference == null || rejectionCode != null) {
                throw new IllegalArgumentException(
                        "Resolved Treasury evidence requires an account and no rejection code"
                );
            }
        } else if (treasuryAccountReference != null || rejectionCode == null) {
            throw new IllegalArgumentException(
                    "Rejected Treasury evidence requires a rejection code and no resolved account"
            );
        }
    }

    public Optional<TreasuryAccountReference> treasuryAccountReference() {
        return Optional.ofNullable(treasuryAccountReference);
    }

    public EvidenceFingerprint allocationIntentFingerprint() {
        return allocationIntentFingerprint;
    }

    public TreasuryResolutionOutcome resolutionOutcome() {
        return resolutionOutcome;
    }

    public String resolverPolicyVersion() {
        return resolverPolicyVersion;
    }

    public Optional<FailureCode> rejectionCode() {
        return Optional.ofNullable(rejectionCode);
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TreasuryAccountResolutionSnapshot that)) {
            return false;
        }
        return Objects.equals(
                treasuryAccountReference,
                that.treasuryAccountReference
        ) && allocationIntentFingerprint.equals(
                that.allocationIntentFingerprint
        ) && resolutionOutcome == that.resolutionOutcome
                && resolverPolicyVersion.equals(that.resolverPolicyVersion)
                && Objects.equals(rejectionCode, that.rejectionCode)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                treasuryAccountReference,
                allocationIntentFingerprint,
                resolutionOutcome,
                resolverPolicyVersion,
                rejectionCode,
                metadata
        );
    }

    @Override
    public String toString() {
        return "TreasuryAccountResolutionSnapshot[outcome="
                + resolutionOutcome
                + ", account=" + treasuryAccountReference
                + ", policyVersion=" + resolverPolicyVersion
                + ", metadata=" + metadata + "]";
    }
}

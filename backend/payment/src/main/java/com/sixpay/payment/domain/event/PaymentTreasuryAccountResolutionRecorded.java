package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Payment has accepted the protected Treasury-account resolution outcome.
 */
public record PaymentTreasuryAccountResolutionRecorded(
        PaymentEventMetadata metadata,
        TreasuryResolutionOutcome outcome,
        String treasuryConfigurationId,
        String configurationVersion,
        String maskedTreasuryAccountReference,
        FailureCode rejectionCode,
        EvidenceFingerprint evidenceFingerprint,
        Instant acceptedAt
) implements PaymentDomainEvent {

    public PaymentTreasuryAccountResolutionRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (treasuryConfigurationId != null) {
            treasuryConfigurationId = treasuryConfigurationId.strip();
            if (treasuryConfigurationId.isEmpty() || treasuryConfigurationId.length() > 256) {
                throw new IllegalArgumentException("treasuryConfigurationId has an invalid length");
            }
        }
        if (configurationVersion != null) {
            configurationVersion = configurationVersion.strip();
            if (configurationVersion.isEmpty() || configurationVersion.length() > 256) {
                throw new IllegalArgumentException("configurationVersion has an invalid length");
            }
        }
        if (maskedTreasuryAccountReference != null) {
            maskedTreasuryAccountReference = maskedTreasuryAccountReference.strip();
            if (maskedTreasuryAccountReference.isEmpty() || maskedTreasuryAccountReference.length() > 256) {
                throw new IllegalArgumentException("maskedTreasuryAccountReference has an invalid length");
            }
        }
        evidenceFingerprint = Objects.requireNonNull(evidenceFingerprint, "evidenceFingerprint");
        acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
        if (outcome == TreasuryResolutionOutcome.RESOLVED) {
            if (treasuryConfigurationId == null
                    || configurationVersion == null
                    || maskedTreasuryAccountReference == null
                    || rejectionCode != null) {
                throw new IllegalArgumentException(
                        "Resolved Treasury event has inconsistent fields"
                );
            }
        } else if (treasuryConfigurationId != null
                || configurationVersion != null
                || maskedTreasuryAccountReference != null
                || rejectionCode == null) {
            throw new IllegalArgumentException(
                    "Rejected Treasury event has inconsistent fields"
            );
        }
    }
}

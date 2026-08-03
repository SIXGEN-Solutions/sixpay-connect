package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.policy.VerificationOutcomePolicy;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable canonical result of a completed Customer Verification.
 *
 * @param outcome global outcome derived from the evidence
 * @param evidence complete immutable banking evidence
 * @param completedAt explicit completion instant
 */
public record CustomerVerificationResult(
        VerificationOutcome outcome,
        VerificationEvidence evidence,
        Instant completedAt
) implements ValueObject {

    public CustomerVerificationResult {
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        evidence = Objects.requireNonNull(
                evidence,
                "evidence is required"
        );
        completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt is required"
        );

        VerificationOutcome derived =
                VerificationOutcomePolicy.determine(evidence.checks());

        if (outcome != derived) {
            throw new CustomerVerificationDomainException(
                    "Verification outcome "
                            + outcome
                            + " is inconsistent with evidence outcome "
                            + derived
            );
        }

        if (completedAt.isBefore(evidence.observedAt())) {
            throw new CustomerVerificationDomainException(
                    "completedAt must not be before observedAt"
            );
        }
    }

    public static CustomerVerificationResult from(
            VerificationEvidence evidence,
            Instant completedAt
    ) {
        Objects.requireNonNull(evidence, "evidence is required");

        return new CustomerVerificationResult(
                VerificationOutcomePolicy.determine(evidence.checks()),
                evidence,
                completedAt
        );
    }
}

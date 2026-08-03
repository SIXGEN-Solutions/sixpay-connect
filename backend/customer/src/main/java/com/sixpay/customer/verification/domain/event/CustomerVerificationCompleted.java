package com.sixpay.customer.verification.domain.event;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Safe domain event emitted once a Customer Verification is completed.
 *
 * <p>The event deliberately excludes raw NIU, customer name, account number,
 * credentials and external-system payloads.</p>
 */
public record CustomerVerificationCompleted(
        UUID eventId,
        CustomerVerificationId verificationId,
        VerificationOutcome outcome,
        List<VerificationCheck> checks,
        VerificationEvidenceFingerprint evidenceFingerprint,
        AccountBindingFingerprint accountBindingFingerprint,
        Instant completedAt
) implements CustomerVerificationDomainEvent {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public CustomerVerificationCompleted {
        eventId = Objects.requireNonNull(eventId, "eventId is required");
        if (NIL_UUID.equals(eventId) || eventId.version() != 4) {
            throw new CustomerVerificationDomainException(
                    "eventId must be a non-nil UUID v4"
            );
        }

        verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        outcome = Objects.requireNonNull(outcome, "outcome is required");
        checks = List.copyOf(
                Objects.requireNonNull(checks, "checks are required")
        );
        if (checks.isEmpty()) {
            throw new CustomerVerificationDomainException(
                    "checks must not be empty"
            );
        }
        evidenceFingerprint = Objects.requireNonNull(
                evidenceFingerprint,
                "evidenceFingerprint is required"
        );
        accountBindingFingerprint = Objects.requireNonNull(
                accountBindingFingerprint,
                "accountBindingFingerprint is required"
        );
        completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt is required"
        );
    }

    @Override
    public Instant occurredAt() {
        return completedAt;
    }
}

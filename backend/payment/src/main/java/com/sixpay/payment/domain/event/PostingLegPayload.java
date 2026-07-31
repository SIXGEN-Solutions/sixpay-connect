package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.PostingLegEvidence;
import com.sixpay.payment.domain.model.evidence.PostingLegStatus;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit safe posting-leg projection.
 */
public record PostingLegPayload(
        PostingLegStatus status,
        String reference,
        Instant effectiveAt,
        FailureCode failureCode
) implements ValueObject {

    public PostingLegPayload {
        status = Objects.requireNonNull(status, "Posting leg status");
        if (reference != null) {
            reference = reference.strip();
            if (reference.isEmpty() || reference.length() > 128) {
                throw new IllegalArgumentException(
                        "Posting leg reference has an invalid length"
                );
            }
        }
    }

    public static PostingLegPayload from(PostingLegEvidence evidence) {
        Objects.requireNonNull(evidence, "Posting leg evidence");
        return new PostingLegPayload(
                evidence.status(),
                evidence.bankEntryReferenceOptional().orElse(null),
                evidence.effectiveAtOptional().orElse(null),
                evidence.failureCodeOptional().orElse(null)
        );
    }

    public Optional<String> referenceOptional() {
        return Optional.ofNullable(reference);
    }

    public Optional<Instant> effectiveAtOptional() {
        return Optional.ofNullable(effectiveAt);
    }

    public Optional<FailureCode> failureCodeOptional() {
        return Optional.ofNullable(failureCode);
    }
}

package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

/**
 * Minimized safe check projection used by evidence events.
 */
public record SafeCheckResult(
        String type,
        EvidenceCheckResult result,
        FailureCode reasonCode
) implements ValueObject {

    public SafeCheckResult {
        Objects.requireNonNull(type, "Check type");
        type = type.strip();
        if (type.isEmpty() || type.length() > 128) {
            throw new IllegalArgumentException(
                    "Check type must contain 1 to 128 characters"
            );
        }
        result = Objects.requireNonNull(result, "Check result");
        if (result == EvidenceCheckResult.PASS
                && reasonCode != null) {
            throw new IllegalArgumentException(
                    "Passing check must not expose a reason code"
            );
        }
    }

    public Optional<FailureCode> reasonCodeOptional() {
        return Optional.ofNullable(reasonCode);
    }
}

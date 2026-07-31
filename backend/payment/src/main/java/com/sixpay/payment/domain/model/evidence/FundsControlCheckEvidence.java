package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Optional;

public record FundsControlCheckEvidence(
        FundsControlCheckType type,
        EvidenceCheckResult result,
        FailureCode reasonCode,
        Instant checkedAt
) implements ValueObject {

    public FundsControlCheckEvidence {
        type = EvidenceValueObjectRules.requireNonNull(
                type,
                "Funds-control check type"
        );
        result = EvidenceValueObjectRules.requireNonNull(
                result,
                "Funds-control check result"
        );
        checkedAt = EvidenceValueObjectRules.requireNonNull(
                checkedAt,
                "Funds-control check instant"
        );

        if (result == EvidenceCheckResult.PASS && reasonCode != null) {
            throw new IllegalArgumentException(
                    "Passing funds-control check must not have a reason code"
            );
        }
    }

    public Optional<FailureCode> reasonCodeOptional() {
        return Optional.ofNullable(reasonCode);
    }
}

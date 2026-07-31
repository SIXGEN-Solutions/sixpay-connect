package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Optional;

public record BankingVerificationCheckEvidence(
        BankingVerificationCheckType type,
        EvidenceCheckResult result,
        FailureCode reasonCode,
        Instant checkedAt
) implements ValueObject {

    public BankingVerificationCheckEvidence {
        type = EvidenceValueObjectRules.requireNonNull(
                type,
                "Banking verification check type"
        );
        result = EvidenceValueObjectRules.requireNonNull(
                result,
                "Banking verification check result"
        );
        if (result == EvidenceCheckResult.PASS && reasonCode != null) {
            throw new IllegalArgumentException(
                    "Passing banking check must not have a reason code"
            );
        }
    }

    public Optional<FailureCode> reasonCodeOptional() {
        return Optional.ofNullable(reasonCode);
    }

    public Optional<Instant> checkedAtOptional() {
        return Optional.ofNullable(checkedAt);
    }
}

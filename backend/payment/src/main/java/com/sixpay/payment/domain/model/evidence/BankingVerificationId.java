package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.UUID;

public record BankingVerificationId(UUID value) implements ValueObject {

    public BankingVerificationId {
        value = EvidenceValueObjectRules.requireNonNilUuid(
                value,
                "Banking verification ID"
        );
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

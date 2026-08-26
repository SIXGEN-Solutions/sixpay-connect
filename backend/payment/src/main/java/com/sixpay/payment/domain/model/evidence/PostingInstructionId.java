package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.UUID;

public record PostingInstructionId(UUID value) implements ValueObject {

    public PostingInstructionId {
        value = EvidenceValueObjectRules.requireNonNilUuid(
                value,
                "Posting instruction ID"
        );
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

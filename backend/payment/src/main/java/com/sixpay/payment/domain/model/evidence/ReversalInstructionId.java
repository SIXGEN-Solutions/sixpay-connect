package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.UUID;

public record ReversalInstructionId(UUID value) implements ValueObject {

    public ReversalInstructionId {
        value = EvidenceValueObjectRules.requireNonNilUuid(
                value,
                "Reversal instruction ID"
        );
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

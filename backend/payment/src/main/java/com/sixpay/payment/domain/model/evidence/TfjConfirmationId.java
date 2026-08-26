package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.UUID;

public record TfjConfirmationId(UUID value) implements ValueObject {

    public TfjConfirmationId {
        value = EvidenceValueObjectRules.requireNonNilUuid(
                value,
                "TFJ confirmation ID"
        );
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

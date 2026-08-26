package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public record ReversalReference(String value) implements ValueObject {

    public ReversalReference {
        value = EvidenceValueObjectRules.requirePrintableAsciiNoWhitespace(
                value,
                1,
                128,
                "Reversal reference"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}

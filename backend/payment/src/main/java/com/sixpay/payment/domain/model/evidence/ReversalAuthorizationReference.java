package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

public record ReversalAuthorizationReference(String value)
        implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$");

    public ReversalAuthorizationReference {
        value = EvidenceValueObjectRules.requirePattern(
                value,
                FORMAT,
                8,
                128,
                "Reversal authorization reference"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}

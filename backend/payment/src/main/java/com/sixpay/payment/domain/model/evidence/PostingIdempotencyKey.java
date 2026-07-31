package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

public record PostingIdempotencyKey(String value) implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{15,127}$");

    public PostingIdempotencyKey {
        value = EvidenceValueObjectRules.requirePattern(
                value,
                FORMAT,
                16,
                128,
                "Posting idempotency key"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}

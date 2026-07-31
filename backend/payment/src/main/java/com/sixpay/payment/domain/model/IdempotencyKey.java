package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Opaque identity of one logical Payment submission.
 *
 * @param value canonical key
 */
public record IdempotencyKey(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$"
    );

    public IdempotencyKey {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                8,
                128,
                "Idempotency key"
        );
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

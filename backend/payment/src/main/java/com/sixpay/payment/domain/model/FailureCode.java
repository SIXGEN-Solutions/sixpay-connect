package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Stable machine-readable Payment failure code.
 *
 * @param value uppercase safe code
 */
public record FailureCode(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Z][A-Z0-9_]{2,63}$"
    );

    public FailureCode {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                3,
                64,
                "Failure code"
        );
    }

    public static FailureCode of(String value) {
        return new FailureCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

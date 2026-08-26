package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Case-sensitive external end-to-end Payment reference.
 *
 * @param value canonical external reference
 */
public record ExternalPaymentReference(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._:/-]{0,126}"
                    + "[A-Za-z0-9])?$"
    );

    public ExternalPaymentReference {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                1,
                128,
                "External Payment reference"
        );
    }

    public static ExternalPaymentReference of(String value) {
        return new ExternalPaymentReference(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

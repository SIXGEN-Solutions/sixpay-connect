package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Stable SIXPAY public Payment reference.
 *
 * @param value PAY-prefixed uppercase Crockford Base32 ULID
 */
public record PublicPaymentReference(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^PAY-[0-9A-HJKMNP-TV-Z]{26}$"
    );

    public PublicPaymentReference {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                30,
                30,
                "Public Payment reference"
        );
    }

    public static PublicPaymentReference of(String value) {
        return new PublicPaymentReference(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

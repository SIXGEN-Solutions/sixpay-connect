package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * SHA-256 digest of the canonical business request.
 *
 * @param value 64 lowercase hexadecimal characters
 */
public record RequestFingerprint(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[0-9a-f]{64}$"
    );

    public RequestFingerprint {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                64,
                64,
                "Request fingerprint"
        );
    }

    public static RequestFingerprint of(String value) {
        return new RequestFingerprint(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

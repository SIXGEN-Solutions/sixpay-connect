package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Immutable TRESOR PAY subscription traceability reference.
 *
 * @param value canonical external subscription reference
 */
public record ExternalSubscriptionReference(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._:/-]{0,126}"
                    + "[A-Za-z0-9])?$"
    );

    public ExternalSubscriptionReference {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                1,
                128,
                "External Subscription reference"
        );
    }

    public static ExternalSubscriptionReference of(String value) {
        return new ExternalSubscriptionReference(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

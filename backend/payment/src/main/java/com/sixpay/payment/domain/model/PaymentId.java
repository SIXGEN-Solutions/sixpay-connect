package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.UUID;

/**
 * Internal opaque identity of one Payment Aggregate Root.
 *
 * @param value non-nil UUID value
 */
public record PaymentId(
        UUID value
) implements ValueObject {

    public PaymentId {
        value = PaymentValueObjectRules.requireNonNilUuid(
                value,
                "Payment ID"
        );
    }

    public static PaymentId from(String value) {
        return new PaymentId(
                PaymentValueObjectRules.parseCanonicalUuid(
                        value,
                        "Payment ID"
                )
        );
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

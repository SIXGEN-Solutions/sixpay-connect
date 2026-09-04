package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Opaque bank-generated confirmation-challenge reference.
 *
 * <p>The reference is internal to SIXPAY and contains no OTP material.</p>
 */
public record ConfirmationChallengeReference(
        String value
) implements ValueObject {

    public ConfirmationChallengeReference {
        value = PaymentValueObjectRules.requireOpaque(
                value,
                1,
                128,
                "Confirmation challenge reference"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * External beneficiary classification used in Treasury allocation intent.
 *
 * @param value canonical reference
 */
public record TreasuryBeneficiaryReference(
        String value
) implements ValueObject, Comparable<TreasuryBeneficiaryReference> {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$"
    );

    public TreasuryBeneficiaryReference {
        value = PaymentValueObjectRules.requirePattern(
                value,
                FORMAT,
                1,
                64,
                "Treasury beneficiary reference"
        );
    }

    public static TreasuryBeneficiaryReference of(String value) {
        return new TreasuryBeneficiaryReference(value);
    }

    @Override
    public int compareTo(TreasuryBeneficiaryReference other) {
        PaymentValueObjectRules.requireNonNull(
                other,
                "Treasury beneficiary reference"
        );
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}

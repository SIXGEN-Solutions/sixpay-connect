package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Canonical code of the target financial institution.
 *
 * @param value uppercase institution code
 */
public record FinancialInstitutionCode(
        String value
) implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Z0-9][A-Z0-9_-]{1,31}$"
    );

    public FinancialInstitutionCode {
        value = PaymentValueObjectRules.requireUppercasePattern(
                value,
                FORMAT,
                2,
                32,
                "Financial institution code"
        );
    }

    public static FinancialInstitutionCode of(String value) {
        return new FinancialInstitutionCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Opaque bank-issued reference identifying accepted customer confirmation.
 */
public record CustomerConfirmationReference(
        String value
) implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9:._/-]{0,127}$");

    public CustomerConfirmationReference {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Customer confirmation reference has an invalid format"
            );
        }
    }

    public static CustomerConfirmationReference of(String value) {
        return new CustomerConfirmationReference(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

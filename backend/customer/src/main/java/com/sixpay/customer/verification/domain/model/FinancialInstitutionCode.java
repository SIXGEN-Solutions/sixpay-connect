package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Canonical code of the financial institution responsible for verification.
 *
 * <p>This Customer-owned type deliberately does not depend on Payment's
 * implementation type. Its normalization and format remain semantically
 * compatible with Payment.</p>
 *
 * @param value uppercase institution code
 */
public record FinancialInstitutionCode(String value)
        implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Z0-9][A-Z0-9_-]{1,31}$"
    );

    public FinancialInstitutionCode {
        if (value == null || value.isBlank()) {
            throw new CustomerVerificationDomainException(
                    "Financial institution code is required"
            );
        }

        value = value.strip().toUpperCase(Locale.ROOT);

        if (!FORMAT.matcher(value).matches()) {
            throw new CustomerVerificationDomainException(
                    "Financial institution code must contain 2 to 32 "
                            + "uppercase letters, digits, underscores "
                            + "or hyphens"
            );
        }
    }

    public static FinancialInstitutionCode of(String value) {
        return new FinancialInstitutionCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

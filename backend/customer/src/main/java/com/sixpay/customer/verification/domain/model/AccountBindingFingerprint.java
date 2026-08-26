package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * Protected and versioned binding to a debtor account reference.
 *
 * @param value opaque fingerprint in {@code v1:<64 lowercase hex>} format
 */
public record AccountBindingFingerprint(String value)
        implements ValueObject {

    private static final Pattern FORMAT = Pattern.compile(
            "^v1:[0-9a-f]{64}$"
    );

    public AccountBindingFingerprint {
        if (value == null || value.isBlank()) {
            throw new CustomerVerificationDomainException(
                    "Account binding fingerprint is required"
            );
        }

        value = value.strip();

        if (!FORMAT.matcher(value).matches()) {
            throw new CustomerVerificationDomainException(
                    "Account binding fingerprint must match "
                            + "v1:<64 lowercase hexadecimal characters>"
            );
        }
    }

    public static AccountBindingFingerprint of(String value) {
        return new AccountBindingFingerprint(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED_ACCOUNT_BINDING]";
    }
}

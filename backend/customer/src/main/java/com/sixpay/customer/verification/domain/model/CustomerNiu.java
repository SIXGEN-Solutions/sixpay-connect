package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalized customer tax identifier used for banking verification.
 *
 * <p>The value is not masked or hashed by this domain object. Protection at
 * rest and masking at API boundaries belong to dedicated adapters.</p>
 *
 * @param value normalized NIU
 */
public record CustomerNiu(String value) implements ValueObject {

    private static final int MAX_LENGTH = 64;

    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Z0-9][A-Z0-9._/-]{0,63}$"
    );

    public CustomerNiu {
        if (value == null) {
            throw new CustomerVerificationDomainException(
                    "Customer NIU is required"
            );
        }

        value = value
                .strip()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (value.isEmpty()) {
            throw new CustomerVerificationDomainException(
                    "Customer NIU must not be blank"
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new CustomerVerificationDomainException(
                    "Customer NIU must not exceed "
                            + MAX_LENGTH + " characters"
            );
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new CustomerVerificationDomainException(
                    "Customer NIU has an invalid format"
            );
        }
    }

    public static CustomerNiu of(String value) {
        return new CustomerNiu(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED_NIU]";
    }
}

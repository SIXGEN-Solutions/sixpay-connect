package com.sixpay.security.domain.authentication;

import java.util.Objects;

/**
 * SIXPAY-owned password policy for LOCAL authentication.
 *
 * <p>This domain object deliberately owns only policy semantics. Password
 * hashing, password history persistence and credential lifecycle state are
 * separate concerns handled by later DA-10 sub-lots.</p>
 */
public record PasswordPolicy(
        int minLength,
        int maxLength,
        int historySize,
        int expirationDays
) {

    public PasswordPolicy {
        if (minLength < 1) {
            throw new IllegalArgumentException(
                    "Password minimum length must be positive"
            );
        }
        if (maxLength < minLength) {
            throw new IllegalArgumentException(
                    "Password maximum length must be greater than or equal to minimum length"
            );
        }
        if (historySize < 0) {
            throw new IllegalArgumentException(
                    "Password history size must not be negative"
            );
        }
        if (expirationDays < 1) {
            throw new IllegalArgumentException(
                    "Password expiration days must be positive"
            );
        }
    }

    public void validate(String password) {
        Objects.requireNonNull(password, "Password must not be null");

        int length = password.length();
        if (length < minLength) {
            throw new IllegalArgumentException(
                    "Password must contain at least "
                            + minLength
                            + " characters"
            );
        }
        if (length > maxLength) {
            throw new IllegalArgumentException(
                    "Password must contain at most "
                            + maxLength
                            + " characters"
            );
        }
    }
}

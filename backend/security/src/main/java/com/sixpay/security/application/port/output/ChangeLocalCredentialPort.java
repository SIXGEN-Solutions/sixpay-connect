package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.authentication.PasswordPolicy;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists a user-owned LOCAL credential replacement.
 *
 * <p>Unlike an administrative reset, this transition clears
 * must-change-password and starts normal password expiration.</p>
 */
public interface ChangeLocalCredentialPort {

    void changePassword(
            UUID userId,
            String passwordHash,
            Instant changedAt,
            PasswordPolicy passwordPolicy
    );
}

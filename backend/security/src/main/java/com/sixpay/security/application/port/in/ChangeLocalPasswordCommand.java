package com.sixpay.security.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Authenticated user-owned LOCAL password change.
 */
public record ChangeLocalPasswordCommand(
        UUID userId,
        String actorSubject,
        String currentPassword,
        String newPassword
) {
    public ChangeLocalPasswordCommand {
        userId = Objects.requireNonNull(
                userId,
                "SIXPAY user id must not be null"
        );
        actorSubject = Objects.requireNonNull(
                actorSubject,
                "Authenticated actor subject must not be null"
        );
        currentPassword = Objects.requireNonNull(
                currentPassword,
                "Current password must not be null"
        );
        newPassword = Objects.requireNonNull(
                newPassword,
                "New password must not be null"
        );
    }
}

package com.sixpay.security.application.model;

import java.util.Objects;

public record DirectoryUserProfile(
        String username,
        String displayName,
        String email,
        String trustDomain,
        String stableSubject,
        DirectoryAccountStatus accountStatus
) {

    public DirectoryUserProfile {
        username = requireNonBlank(
                username,
                "Directory username must not be blank",
                150
        );
        displayName = normalizeOptional(
                displayName,
                255,
                "Directory display name must contain at most 255 characters"
        );
        email = normalizeOptional(
                email,
                320,
                "Directory email must contain at most 320 characters"
        );
        trustDomain = requireNonBlank(
                trustDomain,
                "Directory trust domain must not be blank",
                500
        );
        stableSubject = requireNonBlank(
                stableSubject,
                "Directory stable subject must not be blank",
                255
        );
        accountStatus = Objects.requireNonNull(
                accountStatus,
                "Directory account status must not be null"
        );
    }

    private static String requireNonBlank(
            String value,
            String message,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptional(
            String value,
            int maxLength,
            String message
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}

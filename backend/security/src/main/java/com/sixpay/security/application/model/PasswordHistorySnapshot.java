package com.sixpay.security.application.model;

import com.sixpay.common.validation.Preconditions;
import java.util.List;

public record PasswordHistorySnapshot(
        String currentPasswordHash,
        List<String> recentPasswordHashes
) {
    public PasswordHistorySnapshot {
        currentPasswordHash = Preconditions.requireNonBlank(
                currentPasswordHash,
                "Current password hash must not be blank"
        );
        recentPasswordHashes = List.copyOf(
                Preconditions.requireNonNull(
                        recentPasswordHashes,
                        "Recent password hashes must not be null"
                )
        );
    }
}

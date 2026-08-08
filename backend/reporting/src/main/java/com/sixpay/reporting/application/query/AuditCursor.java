package com.sixpay.reporting.application.query;

import java.util.Objects;

public record AuditCursor(String value) {

    public AuditCursor {
        value = Objects.requireNonNull(value, "cursor is required").strip();
        if (value.isEmpty() || value.length() > 2048) {
            throw new IllegalArgumentException(
                    "cursor must contain between 1 and 2048 characters"
            );
        }
    }

    @Override
    public String toString() {
        return "AuditCursor[PROTECTED]";
    }
}

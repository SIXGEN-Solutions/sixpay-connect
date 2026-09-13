package com.sixpay.administration.domain.model;

import java.time.Instant;

public record DynamicSettingValue(
        String key,
        SettingDomain domain,
        String value,
        long version,
        Instant updatedAt,
        String updatedBy,
        String reason
) {
    public DynamicSettingValue {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
        if (domain == null) throw new IllegalArgumentException("domain is required");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required");
        if (version < 1) throw new IllegalArgumentException("version must be >= 1");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt is required");
        if (updatedBy == null || updatedBy.isBlank()) throw new IllegalArgumentException("updatedBy is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");
    }
}

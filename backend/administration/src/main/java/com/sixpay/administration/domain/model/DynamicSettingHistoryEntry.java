package com.sixpay.administration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DynamicSettingHistoryEntry(
        UUID historyId,
        String key,
        SettingDomain domain,
        String previousValue,
        String newValue,
        long previousVersion,
        long newVersion,
        Instant changedAt,
        String changedBy,
        String reason,
        String operation
) {
}

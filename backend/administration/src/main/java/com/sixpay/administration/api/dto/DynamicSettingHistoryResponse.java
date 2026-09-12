package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.DynamicSettingHistoryEntry;
import java.time.Instant;
import java.util.UUID;

public record DynamicSettingHistoryResponse(
        UUID historyId,
        String key,
        String domain,
        String previousValue,
        String newValue,
        long previousVersion,
        long newVersion,
        Instant changedAt,
        String changedBy,
        String reason,
        String operation
) {
    public static DynamicSettingHistoryResponse from(DynamicSettingHistoryEntry e) {
        return new DynamicSettingHistoryResponse(
                e.historyId(), e.key(), e.domain().name(),
                e.previousValue(), e.newValue(),
                e.previousVersion(), e.newVersion(),
                e.changedAt(), e.changedBy(),
                e.reason(), e.operation()
        );
    }
}

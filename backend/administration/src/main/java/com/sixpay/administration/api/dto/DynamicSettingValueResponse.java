package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.DynamicSettingValue;
import java.time.Instant;

public record DynamicSettingValueResponse(
        String key,
        String domain,
        String value,
        long version,
        Instant updatedAt,
        String updatedBy,
        String reason
) {
    public static DynamicSettingValueResponse from(DynamicSettingValue v) {
        return new DynamicSettingValueResponse(
                v.key(), v.domain().name(), v.value(),
                v.version(), v.updatedAt(), v.updatedBy(), v.reason()
        );
    }
}

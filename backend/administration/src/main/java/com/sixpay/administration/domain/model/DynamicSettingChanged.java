package com.sixpay.administration.domain.model;

import java.time.Instant;

public record DynamicSettingChanged(
        String key,
        SettingDomain domain,
        String value,
        long version,
        Instant changedAt
) {
}

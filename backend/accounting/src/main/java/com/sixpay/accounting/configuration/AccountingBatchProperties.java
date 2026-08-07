package com.sixpay.accounting.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.time.ZoneId;

@ConfigurationProperties(
        prefix = AccountingBatchProperties.PREFIX
)
public record AccountingBatchProperties(
        ZoneId cutoffZone,
        LocalTime cutoffTime
) {
    public static final String PREFIX =
            "sixpay.accounting.batch";

    public AccountingBatchProperties {
        cutoffZone = cutoffZone == null
                ? ZoneId.of("Africa/Douala")
                : cutoffZone;

        cutoffTime = cutoffTime == null
                ? LocalTime.of(23, 0)
                : cutoffTime;
    }
}

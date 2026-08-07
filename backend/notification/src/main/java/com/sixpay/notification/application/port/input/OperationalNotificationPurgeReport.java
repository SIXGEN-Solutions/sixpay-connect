package com.sixpay.notification.application.port.input;

import java.time.Instant;

public record OperationalNotificationPurgeReport(
        int deleted,
        Instant executedAt
) {
}

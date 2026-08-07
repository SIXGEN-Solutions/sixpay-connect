package com.sixpay.notification.application.port.input;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record OperationalNotificationReplayResult(
        UUID notificationId,
        NotificationDeliveryStatus status,
        int replayCount,
        Instant replayedAt
) {
}

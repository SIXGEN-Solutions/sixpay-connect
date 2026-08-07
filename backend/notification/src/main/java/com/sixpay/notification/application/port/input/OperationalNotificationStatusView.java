package com.sixpay.notification.application.port.input;

import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OperationalNotificationStatusView(
        UUID notificationId,
        OperationalNotificationTriggerType triggerType,
        String sourceId,
        String recipientReference,
        NotificationChannel channel,
        NotificationTemplateKey templateKey,
        NotificationDeliveryStatus status,
        int attemptCount,
        int cycleAttemptCount,
        int replayCount,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant deliveredAt,
        Instant lastReplayAt,
        String lastErrorCode,
        String providerReference,
        Instant createdAt,
        String correlationId,
        List<NotificationAttempt> attempts
) {
    public OperationalNotificationStatusView {
        attempts = List.copyOf(attempts);
    }
}

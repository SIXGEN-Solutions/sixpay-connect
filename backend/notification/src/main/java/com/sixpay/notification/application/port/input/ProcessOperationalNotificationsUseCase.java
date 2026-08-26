package com.sixpay.notification.application.port.input;

import java.time.Instant;

public interface ProcessOperationalNotificationsUseCase {

    NotificationProcessingReport processDue(
            Instant dueAt,
            int batchSize
    );
}

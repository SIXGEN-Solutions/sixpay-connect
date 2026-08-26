package com.sixpay.notification.application.port.input;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalNotificationOperationsUseCase {

    Optional<OperationalNotificationStatusView> status(
            UUID notificationId
    );

    List<OperationalNotificationStatusView> findByStatus(
            NotificationDeliveryStatus status,
            int limit
    );

    OperationalNotificationReplayResult replay(
            UUID notificationId,
            OperationalNotificationReplayCommand command
    );
}

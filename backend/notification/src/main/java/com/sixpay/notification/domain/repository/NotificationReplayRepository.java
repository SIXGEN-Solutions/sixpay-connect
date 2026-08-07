package com.sixpay.notification.domain.repository;

import com.sixpay.notification.domain.model.NotificationReplayAudit;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationReplayRepository {

    Optional<OperationalNotificationDelivery> replayDeadLetter(
            NotificationReplayAudit audit
    );

    List<NotificationReplayAudit> findReplaysByNotificationId(
            UUID notificationId
    );
}

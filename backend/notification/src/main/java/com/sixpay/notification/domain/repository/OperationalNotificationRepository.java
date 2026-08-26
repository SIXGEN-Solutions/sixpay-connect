package com.sixpay.notification.domain.repository;

import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalNotificationRepository {

    NotificationSaveResult saveIfAbsent(
            OperationalNotificationDelivery delivery
    );

    OperationalNotificationDelivery save(
            OperationalNotificationDelivery delivery
    );

    Optional<OperationalNotificationDelivery> findById(
            UUID notificationId
    );

    Optional<OperationalNotificationDelivery>
    findByDeduplicationKey(
            NotificationDeduplicationKey deduplicationKey
    );

    List<UUID> findDueNotificationIds(
            Instant dueAt,
            int limit
    );

    Optional<OperationalNotificationDelivery> claimForDispatch(
            UUID notificationId,
            Instant claimedAt
    );
}

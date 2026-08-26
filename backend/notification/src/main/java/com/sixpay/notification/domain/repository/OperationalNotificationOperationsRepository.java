package com.sixpay.notification.domain.repository;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalNotificationOperationsRepository {

    List<UUID> findIdsByStatus(
            NotificationDeliveryStatus status,
            int limit
    );

    long countByStatus(
            NotificationDeliveryStatus status
    );

    long countDue(
            Instant dueAt
    );

    Optional<Instant> findOldestDueAt(
            Instant dueAt
    );

    int purgeTerminal(
            Instant deliveredBefore,
            Instant failedBefore,
            int limit
    );
}

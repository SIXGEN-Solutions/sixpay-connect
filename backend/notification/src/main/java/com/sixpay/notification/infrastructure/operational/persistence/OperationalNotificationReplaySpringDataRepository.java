package com.sixpay.notification.infrastructure.operational.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OperationalNotificationReplaySpringDataRepository
        extends JpaRepository<OperationalNotificationReplayJpaEntity, UUID> {

    List<OperationalNotificationReplayJpaEntity>
    findByNotificationIdOrderByRequestedAtAsc(UUID notificationId);
}

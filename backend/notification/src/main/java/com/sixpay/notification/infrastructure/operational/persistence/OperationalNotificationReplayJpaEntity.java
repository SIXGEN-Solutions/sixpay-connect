package com.sixpay.notification.infrastructure.operational.persistence;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationReplayAudit;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operational_notification_replays", schema = "sixpay")
public class OperationalNotificationReplayJpaEntity {

    @Id
    @Column(name = "replay_id", nullable = false)
    private UUID replayId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "operator_reference", nullable = false, length = 128)
    private String operatorReference;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 32)
    private NotificationDeliveryStatus previousStatus;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    protected OperationalNotificationReplayJpaEntity() {
    }

    static OperationalNotificationReplayJpaEntity from(
            NotificationReplayAudit audit
    ) {
        var entity = new OperationalNotificationReplayJpaEntity();
        entity.replayId = audit.replayId();
        entity.notificationId = audit.notificationId();
        entity.operatorReference = audit.operatorReference();
        entity.reason = audit.reason();
        entity.previousStatus = audit.previousStatus();
        entity.requestedAt = audit.requestedAt();
        return entity;
    }

    UUID replayId() { return replayId; }
    UUID notificationId() { return notificationId; }
    String operatorReference() { return operatorReference; }
    String reason() { return reason; }
    NotificationDeliveryStatus previousStatus() { return previousStatus; }
    Instant requestedAt() { return requestedAt; }
}

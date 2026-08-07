package com.sixpay.notification.infrastructure.operational.persistence;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "operational_notification_deliveries",
        schema = "sixpay"
)
public class OperationalNotificationJpaEntity {

    @Id
    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 64)
    private OperationalNotificationTriggerType triggerType;

    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 64)
    private NotificationRecipientType recipientType;

    @Column(
            name = "recipient_reference",
            nullable = false,
            length = 128
    )
    private String recipientReference;

    @Column(name = "recipient_locale", nullable = false, length = 32)
    private String recipientLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_key", nullable = false, length = 96)
    private NotificationTemplateKey templateKey;

    @Column(
            name = "deduplication_key",
            nullable = false,
            unique = true,
            length = 64
    )
    private String deduplicationKey;

    @Column(
            name = "template_variables",
            nullable = false,
            columnDefinition = "text"
    )
    private String templateVariables;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_error_code", length = 128)
    private String lastErrorCode;

    @Column(name = "provider_reference", length = 256)
    private String providerReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "correlation_id", nullable = false, length = 150)
    private String correlationId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected OperationalNotificationJpaEntity() {
    }

    void synchronize(
            OperationalNotificationDelivery delivery,
            String encodedVariables
    ) {
        var intent = delivery.intent();

        notificationId = intent.notificationId();
        triggerType = intent.source().triggerType();
        sourceId = intent.source().sourceId();
        recipientType = intent.recipient().type();
        recipientReference = intent.recipient().reference();
        recipientLocale = intent.recipient()
                .locale()
                .toLanguageTag();
        channel = intent.channel();
        templateKey = intent.templateKey();
        deduplicationKey =
                intent.deduplicationKey().value();
        templateVariables = encodedVariables;
        status = intent.status();
        attemptCount = delivery.attemptCount();
        nextAttemptAt = delivery.nextAttemptAt();
        lastAttemptAt = delivery.lastAttemptAt();
        deliveredAt = delivery.deliveredAt();
        lastErrorCode = delivery.lastErrorCode();
        providerReference = delivery.providerReference();
        createdAt = intent.createdAt();
        correlationId = intent.correlationId();
    }

    UUID notificationId() {
        return notificationId;
    }

    OperationalNotificationTriggerType triggerType() {
        return triggerType;
    }

    String sourceId() {
        return sourceId;
    }

    NotificationRecipientType recipientType() {
        return recipientType;
    }

    String recipientReference() {
        return recipientReference;
    }

    String recipientLocale() {
        return recipientLocale;
    }

    NotificationChannel channel() {
        return channel;
    }

    NotificationTemplateKey templateKey() {
        return templateKey;
    }

    String deduplicationKey() {
        return deduplicationKey;
    }

    String templateVariables() {
        return templateVariables;
    }

    NotificationDeliveryStatus status() {
        return status;
    }

    int attemptCount() {
        return attemptCount;
    }

    Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    Instant deliveredAt() {
        return deliveredAt;
    }

    String lastErrorCode() {
        return lastErrorCode;
    }

    String providerReference() {
        return providerReference;
    }

    Instant createdAt() {
        return createdAt;
    }

    String correlationId() {
        return correlationId;
    }
}

package com.sixpay.notification.domain.model;

import java.time.Instant;

public sealed interface OperationalNotificationTrigger
        permits PaymentPostedNotificationTrigger,
        AccountingBatchCompletedNotificationTrigger {

    OperationalNotificationTriggerType type();

    NotificationSourceReference sourceReference();

    Instant occurredAt();

    String correlationId();
}

package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationTrigger;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;

import java.util.Objects;

public final class OperationalNotificationRoutingPolicy {

    public NotificationRoute route(
            OperationalNotificationTrigger trigger
    ) {
        Objects.requireNonNull(trigger, "trigger");

        return switch (trigger.type()) {
            case PAYMENT_POSTED ->
                    new NotificationRoute(
                            NotificationChannel.EMAIL,
                            NotificationTemplateKey
                                    .PAYMENT_POSTED_ADMIN_V1
                    );

            case ACCOUNTING_BATCH_COMPLETED ->
                    new NotificationRoute(
                            NotificationChannel.EMAIL,
                            NotificationTemplateKey
                                    .ACCOUNTING_BATCH_COMPLETED_ADMIN_V1
                    );
        };
    }

    public record NotificationRoute(
            NotificationChannel channel,
            NotificationTemplateKey templateKey
    ) {
        public NotificationRoute {
            channel = Objects.requireNonNull(
                    channel,
                    "channel"
            );
            templateKey = Objects.requireNonNull(
                    templateKey,
                    "templateKey"
            );
        }
    }
}

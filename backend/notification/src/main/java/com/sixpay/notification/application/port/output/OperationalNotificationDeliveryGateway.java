package com.sixpay.notification.application.port.output;

import com.sixpay.notification.domain.model.NotificationIntent;

public interface OperationalNotificationDeliveryGateway {

    NotificationDispatchResult deliver(
            NotificationIntent notification
    );
}

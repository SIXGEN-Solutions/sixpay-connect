package com.sixpay.notification.application.port.input;

import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.OperationalNotificationTrigger;

import java.util.List;

public interface OperationalNotificationTriggerUseCase {

    List<NotificationIntent> plan(
            OperationalNotificationTrigger trigger
    );
}

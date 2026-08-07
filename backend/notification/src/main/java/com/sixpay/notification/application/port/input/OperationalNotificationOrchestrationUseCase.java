package com.sixpay.notification.application.port.input;

import com.sixpay.notification.domain.model.OperationalNotificationTrigger;

public interface OperationalNotificationOrchestrationUseCase {

    NotificationRegistrationResult accept(
            OperationalNotificationTrigger trigger
    );
}

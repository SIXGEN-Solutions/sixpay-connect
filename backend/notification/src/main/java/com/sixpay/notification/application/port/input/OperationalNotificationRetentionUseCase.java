package com.sixpay.notification.application.port.input;

import java.time.Instant;

public interface OperationalNotificationRetentionUseCase {

    OperationalNotificationPurgeReport purge(
            Instant now
    );
}

package com.sixpay.notification.application.port.input;

public record NotificationRegistrationResult(
        int planned,
        int persisted,
        boolean successful,
        String errorCode
) {
    public static NotificationRegistrationResult success(
            int planned,
            int persisted
    ) {
        return new NotificationRegistrationResult(
                planned,
                persisted,
                true,
                null
        );
    }

    public static NotificationRegistrationResult failure(
            String errorCode
    ) {
        return new NotificationRegistrationResult(
                0,
                0,
                false,
                errorCode == null || errorCode.isBlank()
                        ? "NOTIFICATION_REGISTRATION_FAILED"
                        : errorCode.strip()
        );
    }
}

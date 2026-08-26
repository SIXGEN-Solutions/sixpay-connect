package com.sixpay.notification.application.port.input;

public record NotificationProcessingReport(
        int candidates,
        int processed,
        int delivered,
        int accepted,
        int retryScheduled,
        int permanentlyFailed,
        int deadLettered,
        int skipped
) {
}

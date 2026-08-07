package com.sixpay.notification.application.port.output;

public interface OperationalNotificationOperationsTelemetry {

    void recordReplay();

    void recordPurged(
            int count
    );

    OperationalNotificationOperationsTelemetry NOOP =
            new OperationalNotificationOperationsTelemetry() {
                @Override
                public void recordReplay() {
                }

                @Override
                public void recordPurged(
                        int count
                ) {
                }
            };
}

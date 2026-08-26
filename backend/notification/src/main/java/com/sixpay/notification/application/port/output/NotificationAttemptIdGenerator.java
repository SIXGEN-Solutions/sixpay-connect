package com.sixpay.notification.application.port.output;

import java.util.UUID;

@FunctionalInterface
public interface NotificationAttemptIdGenerator {

    UUID nextId();
}

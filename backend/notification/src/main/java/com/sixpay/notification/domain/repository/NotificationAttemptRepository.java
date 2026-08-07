package com.sixpay.notification.domain.repository;

import com.sixpay.notification.domain.model.NotificationAttempt;

import java.util.List;
import java.util.UUID;

public interface NotificationAttemptRepository {

    NotificationAttempt append(
            NotificationAttempt attempt
    );

    List<NotificationAttempt> findByNotificationId(
            UUID notificationId
    );
}

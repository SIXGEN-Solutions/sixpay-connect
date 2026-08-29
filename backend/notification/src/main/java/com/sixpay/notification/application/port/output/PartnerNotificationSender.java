package com.sixpay.notification.application.port.output;

import com.sixpay.notification.application.model.PartnerDecisionNotification;

@FunctionalInterface
public interface PartnerNotificationSender {

    void send(PartnerDecisionNotification notification);
}

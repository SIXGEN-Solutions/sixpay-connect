package com.sixpay.notification.application.port.output;

import com.sixpay.notification.domain.model.NotificationRecipient;

import java.util.List;

public interface SixPayAdminRecipientResolver {

    List<NotificationRecipient> resolveActiveRecipients();
}

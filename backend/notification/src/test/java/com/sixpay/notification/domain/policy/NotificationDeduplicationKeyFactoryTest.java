package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NotificationDeduplicationKeyFactoryTest {

    private final NotificationDeduplicationKeyFactory factory =
            new NotificationDeduplicationKeyFactory();

    @Test
    void sameFunctionalNotificationProducesSameKey() {
        var source = new NotificationSourceReference(
                OperationalNotificationTriggerType.PAYMENT_POSTED,
                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
        );

        var recipient = new NotificationRecipient(
                NotificationRecipientType.SIXPAY_ADMIN,
                "operations-admin",
                Locale.FRENCH
        );

        var first = factory.create(
                source,
                recipient,
                NotificationChannel.EMAIL,
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1
        );

        var second = factory.create(
                source,
                recipient,
                NotificationChannel.EMAIL,
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1
        );

        assertEquals(first, second);
    }

    @Test
    void differentRecipientProducesDifferentKey() {
        var source = new NotificationSourceReference(
                OperationalNotificationTriggerType.PAYMENT_POSTED,
                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
        );

        var first = factory.create(
                source,
                new NotificationRecipient(
                        NotificationRecipientType.SIXPAY_ADMIN,
                        "operations-admin",
                        Locale.FRENCH
                ),
                NotificationChannel.EMAIL,
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1
        );

        var second = factory.create(
                source,
                new NotificationRecipient(
                        NotificationRecipientType.SIXPAY_ADMIN,
                        "risk-admin",
                        Locale.FRENCH
                ),
                NotificationChannel.EMAIL,
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1
        );

        assertNotEquals(first, second);
    }
}

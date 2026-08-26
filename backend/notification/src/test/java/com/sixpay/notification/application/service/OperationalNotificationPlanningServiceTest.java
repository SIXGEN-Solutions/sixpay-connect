package com.sixpay.notification.application.service;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.PaymentPostedNotificationTrigger;
import com.sixpay.notification.domain.policy.NotificationDeduplicationKeyFactory;
import com.sixpay.notification.domain.policy.OperationalNotificationRoutingPolicy;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OperationalNotificationPlanningServiceTest {

    @Test
    void paymentPostedPlansAdminEmailWithoutProviderAddress() {
        UUID notificationId = UUID.fromString(
                "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
        );

        var service = new OperationalNotificationPlanningService(
                () -> List.of(
                        new NotificationRecipient(
                                NotificationRecipientType.SIXPAY_ADMIN,
                                "operations-admin",
                                Locale.FRENCH
                        )
                ),
                () -> notificationId,
                new OperationalNotificationRoutingPolicy(),
                new NotificationDeduplicationKeyFactory(),
                new NotificationTemplateVariableMapper(),
                new OperationalNotificationTemplateCatalog(),
                Clock.fixed(
                        Instant.parse("2026-08-07T16:00:00Z"),
                        ZoneOffset.UTC
                )
        );

        var intents = service.plan(
                new PaymentPostedNotificationTrigger(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        ),
                        "PAY-20260807-0001",
                        "TRESORPAY",
                        new BigDecimal("10000"),
                        Currency.getInstance("XAF"),
                        Instant.parse("2026-08-07T15:55:00Z"),
                        "corr-payment-posted-1"
                )
        );

        assertEquals(1, intents.size());

        var intent = intents.getFirst();

        assertEquals(notificationId, intent.notificationId());
        assertEquals(
                NotificationChannel.EMAIL,
                intent.channel()
        );
        assertEquals(
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1,
                intent.templateKey()
        );
        assertEquals(
                NotificationDeliveryStatus.PENDING,
                intent.status()
        );
        assertEquals(
                "operations-admin",
                intent.recipient().reference()
        );

        assertFalse(
                intent.templateVariables()
                        .containsKey("accountNumber")
        );

        assertFalse(
                intent.templateVariables()
                        .containsKey("niu")
        );
    }
}

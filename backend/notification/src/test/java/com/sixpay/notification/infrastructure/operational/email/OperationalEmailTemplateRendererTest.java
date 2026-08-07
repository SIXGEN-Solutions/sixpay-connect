package com.sixpay.notification.infrastructure.operational.email;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalEmailTemplateRendererTest {

    private final OperationalEmailTemplateRenderer renderer =
            new OperationalEmailTemplateRenderer(
                    new OperationalNotificationTemplateCatalog()
            );

    @Test
    void rendersVersionedPaymentTemplate() {
        var rendered =
                renderer.render(
                        paymentIntent(),
                        "[SIXPAY]"
                );

        assertTrue(
                rendered.subject().contains(
                        "Paiement comptabilisé"
                )
        );
        assertTrue(
                rendered.body().contains(
                        "PAY-20260807-0001"
                )
        );
        assertTrue(
                rendered.body().contains(
                        "10000 XAF"
                )
        );
        assertFalse(
                rendered.body().contains("{{")
        );
    }

    @Test
    void rejectsIncompleteVariableContract() {
        NotificationIntent original =
                paymentIntent();

        NotificationIntent invalid =
                new NotificationIntent(
                        original.notificationId(),
                        original.source(),
                        original.recipient(),
                        original.channel(),
                        original.templateKey(),
                        original.deduplicationKey(),
                        Map.of(
                                "paymentId",
                                original.templateVariables()
                                        .get("paymentId")
                        ),
                        original.status(),
                        original.createdAt(),
                        original.correlationId()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> renderer.render(
                        invalid,
                        "[SIXPAY]"
                )
        );
    }

    static NotificationIntent paymentIntent() {
        return new NotificationIntent(
                UUID.fromString(
                        "0c7945f9-26b4-4caa-a75c-dc7985c68c3a"
                ),
                new NotificationSourceReference(
                        OperationalNotificationTriggerType
                                .PAYMENT_POSTED,
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                new NotificationRecipient(
                        NotificationRecipientType.SIXPAY_ADMIN,
                        "operations-admin",
                        Locale.FRENCH
                ),
                NotificationChannel.EMAIL,
                NotificationTemplateKey
                        .PAYMENT_POSTED_ADMIN_V1,
                new NotificationDeduplicationKey(
                        "a".repeat(64)
                ),
                Map.of(
                        "paymentId",
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a",
                        "paymentReference",
                        "PAY-20260807-0001",
                        "partnerId",
                        "TRESORPAY",
                        "amount",
                        "10000",
                        "currency",
                        "XAF",
                        "postedAt",
                        "2026-08-07T15:55:00Z"
                ),
                NotificationDeliveryStatus.DISPATCHING,
                Instant.parse(
                        "2026-08-07T16:00:00Z"
                ),
                "corr-notification-email-1"
        );
    }
}

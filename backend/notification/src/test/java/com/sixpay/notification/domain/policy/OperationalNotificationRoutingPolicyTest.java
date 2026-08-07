package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.AccountingBatchCompletedNotificationTrigger;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.PaymentPostedNotificationTrigger;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalNotificationRoutingPolicyTest {

    private final OperationalNotificationRoutingPolicy policy =
            new OperationalNotificationRoutingPolicy();

    @Test
    void routesPaymentPostedToAdminEmailTemplate() {
        var route = policy.route(
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

        assertEquals(
                NotificationChannel.EMAIL,
                route.channel()
        );
        assertEquals(
                NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1,
                route.templateKey()
        );
    }

    @Test
    void routesAccountingCompletedToAdminEmailTemplate() {
        var route = policy.route(
                new AccountingBatchCompletedNotificationTrigger(
                        UUID.fromString(
                                "fd7791ae-1bc0-4cd0-af0a-667a6c986547"
                        ),
                        LocalDate.of(2026, 8, 7),
                        "LAREGIONALE",
                        25,
                        Instant.parse("2026-08-07T23:10:00Z"),
                        "corr-accounting-completed-1"
                )
        );

        assertEquals(
                NotificationChannel.EMAIL,
                route.channel()
        );
        assertEquals(
                NotificationTemplateKey
                        .ACCOUNTING_BATCH_COMPLETED_ADMIN_V1,
                route.templateKey()
        );
    }
}

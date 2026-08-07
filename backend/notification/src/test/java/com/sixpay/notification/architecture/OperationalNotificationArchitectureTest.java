package com.sixpay.notification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/notification"
    );

    @Test
    void operationalTriggersDoNotDependOnPaymentOrAccountingModules()
            throws Exception {

        for (String relativePath : List.of(
                "domain/model/OperationalNotificationTrigger.java",
                "domain/model/PaymentPostedNotificationTrigger.java",
                "domain/model/AccountingBatchCompletedNotificationTrigger.java",
                "application/service/OperationalNotificationPlanningService.java"
        )) {
            String source = Files.readString(
                    ROOT.resolve(relativePath)
            );

            assertFalse(
                    source.contains(
                            "import com.sixpay.payment."
                    ),
                    () -> relativePath
                            + " must not depend on Payment"
            );

            assertFalse(
                    source.contains(
                            "import com.sixpay.accounting."
                    ),
                    () -> relativePath
                            + " must not depend on Accounting"
            );
        }
    }

    @Test
    void lotFiveSevenOneIntroducesNoDeliveryTransport()
            throws Exception {

        for (String relativePath : List.of(
                "application/service/OperationalNotificationPlanningService.java",
                "domain/policy/OperationalNotificationRoutingPolicy.java",
                "events/OperationalNotificationEventTypes.java"
        )) {
            String source = Files.readString(
                    ROOT.resolve(relativePath)
            );

            for (String forbidden : List.of(
                    "KafkaTemplate",
                    "@KafkaListener",
                    "JavaMailSender",
                    "RestClient",
                    "WebClient"
            )) {
                assertFalse(
                        source.contains(forbidden),
                        () -> relativePath
                                + " unexpectedly contains "
                                + forbidden
                );
            }
        }
    }

    @Test
    void tresorPayCallbackIsNotOwnedByNotification()
            throws Exception {
        String eventTypes = Files.readString(
                ROOT.resolve(
                        "events/OperationalNotificationEventTypes.java"
                )
        );

        assertFalse(
                eventTypes.contains(
                        "callback"
                )
        );

        assertFalse(
                eventTypes.contains(
                        "TRESORPAY_CALLBACK"
                )
        );
    }

    @Test
    void templatesUseOnlyVersionedOperationalKeys()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "domain/model/NotificationTemplateKey.java"
                )
        );

        assertTrue(
                source.contains(
                        "PAYMENT_POSTED_ADMIN_V1"
                )
        );

        assertTrue(
                source.contains(
                        "ACCOUNTING_BATCH_COMPLETED_ADMIN_V1"
                )
        );
    }
}

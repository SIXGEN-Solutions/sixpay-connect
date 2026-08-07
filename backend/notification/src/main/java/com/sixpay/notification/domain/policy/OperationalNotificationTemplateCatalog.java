package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationTemplateDefinition;
import com.sixpay.notification.domain.model.NotificationTemplateKey;

import java.util.Map;
import java.util.Set;

public final class OperationalNotificationTemplateCatalog {

    private static final Map<
            NotificationTemplateKey,
            NotificationTemplateDefinition
            > DEFINITIONS = Map.of(
            NotificationTemplateKey.PAYMENT_POSTED_ADMIN_V1,
            new NotificationTemplateDefinition(
                    NotificationTemplateKey
                            .PAYMENT_POSTED_ADMIN_V1,
                    "notification/templates/"
                            + "payment-posted-admin-v1.txt",
                    Set.of(
                            "paymentId",
                            "paymentReference",
                            "partnerId",
                            "amount",
                            "currency",
                            "postedAt"
                    )
            ),
            NotificationTemplateKey
                    .ACCOUNTING_BATCH_COMPLETED_ADMIN_V1,
            new NotificationTemplateDefinition(
                    NotificationTemplateKey
                            .ACCOUNTING_BATCH_COMPLETED_ADMIN_V1,
                    "notification/templates/"
                            + "accounting-batch-completed-admin-v1.txt",
                    Set.of(
                            "batchId",
                            "businessDate",
                            "financialInstitutionCode",
                            "itemCount",
                            "completedAt"
                    )
            )
    );

    public NotificationTemplateDefinition definition(
            NotificationTemplateKey key
    ) {
        NotificationTemplateDefinition definition =
                DEFINITIONS.get(key);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown operational notification template: "
                            + key
            );
        }

        return definition;
    }
}

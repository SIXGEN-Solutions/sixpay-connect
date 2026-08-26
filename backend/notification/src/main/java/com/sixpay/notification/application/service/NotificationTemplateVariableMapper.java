package com.sixpay.notification.application.service;

import com.sixpay.notification.domain.model.AccountingBatchCompletedNotificationTrigger;
import com.sixpay.notification.domain.model.OperationalNotificationTrigger;
import com.sixpay.notification.domain.model.PaymentPostedNotificationTrigger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NotificationTemplateVariableMapper {

    public Map<String, String> map(
            OperationalNotificationTrigger trigger
    ) {
        return switch (trigger) {
            case PaymentPostedNotificationTrigger payment ->
                    paymentVariables(payment);

            case AccountingBatchCompletedNotificationTrigger batch ->
                    accountingVariables(batch);
        };
    }

    private Map<String, String> paymentVariables(
            PaymentPostedNotificationTrigger trigger
    ) {
        Map<String, String> variables =
                new LinkedHashMap<>();

        variables.put(
                "paymentId",
                trigger.paymentId().toString()
        );
        variables.put(
                "paymentReference",
                trigger.publicPaymentReference()
        );
        variables.put(
                "partnerId",
                trigger.partnerId()
        );
        variables.put(
                "amount",
                trigger.amount().toPlainString()
        );
        variables.put(
                "currency",
                trigger.currency().getCurrencyCode()
        );
        variables.put(
                "postedAt",
                trigger.postedAt().toString()
        );

        return Map.copyOf(variables);
    }

    private Map<String, String> accountingVariables(
            AccountingBatchCompletedNotificationTrigger trigger
    ) {
        Map<String, String> variables =
                new LinkedHashMap<>();

        variables.put(
                "batchId",
                trigger.batchId().toString()
        );
        variables.put(
                "businessDate",
                trigger.businessDate().toString()
        );
        variables.put(
                "financialInstitutionCode",
                trigger.financialInstitutionCode()
        );
        variables.put(
                "itemCount",
                Integer.toString(
                        trigger.itemCount()
                )
        );
        variables.put(
                "completedAt",
                trigger.completedAt().toString()
        );

        return Map.copyOf(variables);
    }
}

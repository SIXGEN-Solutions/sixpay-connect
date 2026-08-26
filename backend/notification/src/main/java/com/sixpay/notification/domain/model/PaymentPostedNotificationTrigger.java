package com.sixpay.notification.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public record PaymentPostedNotificationTrigger(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        BigDecimal amount,
        Currency currency,
        Instant postedAt,
        String correlationId
) implements OperationalNotificationTrigger {

    public PaymentPostedNotificationTrigger {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId"
        );

        publicPaymentReference = required(
                publicPaymentReference,
                "publicPaymentReference"
        );

        partnerId = required(
                partnerId,
                "partnerId"
        );

        amount = Objects.requireNonNull(
                amount,
                "amount"
        );

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "amount must be positive"
            );
        }

        currency = Objects.requireNonNull(
                currency,
                "currency"
        );

        postedAt = Objects.requireNonNull(
                postedAt,
                "postedAt"
        );

        correlationId = required(
                correlationId,
                "correlationId"
        );
    }

    @Override
    public OperationalNotificationTriggerType type() {
        return OperationalNotificationTriggerType
                .PAYMENT_POSTED;
    }

    @Override
    public NotificationSourceReference
    sourceReference() {
        return new NotificationSourceReference(
                type(),
                paymentId.toString()
        );
    }

    @Override
    public Instant occurredAt() {
        return postedAt;
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }

        return value.strip();
    }
}

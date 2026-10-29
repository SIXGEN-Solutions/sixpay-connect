package com.sixpay.payment.application.port.output.callback;

import com.sixpay.common.context.CorrelationId;
import java.util.Objects;
import java.util.UUID;

public record PaymentStatusCallbackDelivery(
        String callbackUrl,
        CorrelationId correlationId,
        UUID deliveryId,
        int deliveryAttempt,
        PaymentStatusCallbackMessage message
) {
    public PaymentStatusCallbackDelivery {
        if (callbackUrl == null || !callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Callback URL must use HTTPS");
        }
        correlationId = Objects.requireNonNull(correlationId, "Correlation ID");
        deliveryId = Objects.requireNonNull(deliveryId, "Delivery ID");
        if (deliveryAttempt < 1 || deliveryAttempt > 12) {
            throw new IllegalArgumentException("Delivery attempt must be between 1 and 12");
        }
        message = Objects.requireNonNull(message, "Callback message");
    }
}

package com.sixpay.payment.application.port.output.callback;

import com.sixpay.common.context.CorrelationId;

import java.util.Objects;

public record PaymentStatusCallbackDelivery(
        String callbackUrl,
        CorrelationId correlationId,
        PaymentStatusCallbackMessage message
) {

    public PaymentStatusCallbackDelivery {
        if (callbackUrl == null
                || !callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "Callback URL must use HTTPS"
            );
        }
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
        message = Objects.requireNonNull(
                message,
                "Callback message"
        );
    }
}

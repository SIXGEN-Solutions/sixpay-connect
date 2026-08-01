package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentId;

import java.time.Instant;
import java.util.Objects;

public record StartAuthorizationCommand(
        PaymentId paymentId,
        Instant startedAt
) {
    public StartAuthorizationCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        startedAt = Objects.requireNonNull(
                startedAt,
                "Authorization start instant"
        );
    }
}

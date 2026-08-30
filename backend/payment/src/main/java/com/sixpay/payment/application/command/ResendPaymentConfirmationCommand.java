package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

/**
 * Public resend-confirmation application command.
 *
 * <p>Resend maps to the approved Core Banking challenge replacement
 * operation. No OTP value is accepted by this command.</p>
 */
public record ResendPaymentConfirmationCommand(
        PublicPaymentReference paymentReference,
        CorrelationId correlationId,
        IdempotencyKey idempotencyKey
) {
    public ResendPaymentConfirmationCommand {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Idempotency key"
        );
    }
}

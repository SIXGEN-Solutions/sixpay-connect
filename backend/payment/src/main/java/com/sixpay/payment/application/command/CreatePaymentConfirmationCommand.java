package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

/**
 * Public create-confirmation application command.
 *
 * <p>No TRESOR PAY request payload is reproduced here. The business context
 * required by Core Banking is derived from the existing Payment aggregate.</p>
 */
public record CreatePaymentConfirmationCommand(
        PublicPaymentReference paymentReference,
        CorrelationId correlationId,
        IdempotencyKey idempotencyKey
) {
    public CreatePaymentConfirmationCommand {
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

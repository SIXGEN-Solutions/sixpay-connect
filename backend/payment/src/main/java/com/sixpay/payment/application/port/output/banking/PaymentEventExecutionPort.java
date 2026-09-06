package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;

import java.util.Objects;

/**
 * Payment-owned application port for protected synchronous T0 execution.
 *
 * <p>The provider payload remains owned by Payment and is not exposed from the
 * provider-neutral integration module.</p>
 */
public interface PaymentEventExecutionPort {

    AmplitudePaymentEventResult execute(
            PaymentEventExecutionCommand command
    );

    record PaymentEventExecutionCommand(
            AmplitudePaymentEventRequest request,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    ) {
        public PaymentEventExecutionCommand {
            request = Objects.requireNonNull(
                    request,
                    "Amplitude Payment event request"
            );
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
        }
    }
}

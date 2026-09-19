package com.sixpay.payment.infrastructure.callback.tresorpay;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * Physical TRESOR PAY callback payload.
 *
 * <p>The application callback fact remains provider-neutral. This transport
 * representation preserves the approved TRESOR PAY wire field
 * {@code tresorPayPaymentReference} without leaking that vocabulary into the
 * application port.</p>
 */
public record TresorPayPaymentCallbackPayload(
        String schemaVersion,
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID correlationId,
        UUID causationId,
        UUID paymentId,
        String paymentReference,
        @JsonProperty("tresorPayPaymentReference")
        String tresorPayPaymentReference,
        String financialInstitutionCode,
        long paymentVersion,
        Object data
) {

    public static TresorPayPaymentCallbackPayload from(
            PaymentStatusCallbackMessage message
    ) {
        return new TresorPayPaymentCallbackPayload(
                message.schemaVersion(),
                message.eventId(),
                message.eventType(),
                message.occurredAt(),
                message.correlationId(),
                message.causationId(),
                message.paymentId(),
                message.paymentReference(),
                message.externalPaymentReference(),
                message.financialInstitutionCode(),
                message.paymentVersion(),
                message.data()
        );
    }
}

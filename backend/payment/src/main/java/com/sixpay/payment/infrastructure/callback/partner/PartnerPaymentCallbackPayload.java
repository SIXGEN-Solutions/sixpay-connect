package com.sixpay.payment.infrastructure.callback.partner;

import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * Physical Partner callback payload aligned with the active Partner contract.
 *
 */
public record PartnerPaymentCallbackPayload(
        String schemaVersion,
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID correlationId,
        UUID causationId,
        UUID paymentId,
        String paymentReference,
        String externalPaymentReference,
        String financialInstitutionCode,
        long paymentVersion,
        Object data
) {

    public static PartnerPaymentCallbackPayload from(
            PaymentStatusCallbackMessage message
    ) {
        return new PartnerPaymentCallbackPayload(
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
